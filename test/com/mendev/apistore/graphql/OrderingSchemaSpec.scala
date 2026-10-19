package com.mendev.apistore.graphql

import com.mendev.apistore.shared.AppComponentsSuite
import com.mendev.apistore.shared.db.TestDatabase
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.time.Instant
import java.util.UUID

class OrderingSchemaSpec extends PlaySpec with AppComponentsSuite with BeforeAndAfterEach {

  override def beforeEach(): Unit =
    List(
      "ordering_order_status_history",
      "ordering_order_item",
      "ordering_order",
      "shared_event",
      "shopping_cart_item",
      "shopping_cart",
      "catalog_product"
    ).foreach(table => TestDatabase.update(s"DELETE FROM $table"))

  private val password = "Secret1234"

  private lazy val clientToken: String = {
    signUp("checkout-client@apistore.com")
    signIn("checkout-client@apistore.com")
  }

  private lazy val managerToken: String = {
    signUp("checkout-manager@apistore.com")
    TestDatabase.update("UPDATE identity_user SET role = 'MANAGER' WHERE email = 'checkout-manager@apistore.com'")
    signIn("checkout-manager@apistore.com")
  }

  private lazy val otherClientToken: String = {
    signUp("orders-other-client@apistore.com")
    signIn("orders-other-client@apistore.com")
  }

  private def placeOrder(token: String, productId: Long): String = {
    graphql(token, s"mutation { addToCart(productId: $productId, quantity: 1) { total } }")
    (graphql(token, "mutation { checkout { id } }") \ "data" \ "checkout" \ "id").as[String]
  }

  private def signUp(email: String): Unit = {
    val body = Json.obj("email" -> email, "password" -> password, "firstName" -> "Ana", "lastName" -> "Ruiz")
    status(route(app, FakeRequest(POST, "/auth/sign-up").withJsonBody(body)).get) mustBe CREATED
  }

  private def signIn(email: String): String = {
    val body = Json.obj("email" -> email, "password" -> password)
    (contentAsJson(route(app, FakeRequest(POST, "/auth/sign-in").withJsonBody(body)).get) \ "access_token").as[String]
  }

  private def insertProduct(id: Long): Unit =
    TestDatabase.update(
      s"""INSERT INTO catalog_product
         |(id, name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at)
         |VALUES ($id, 'Mouse', 'Wireless mouse', 1, 19.99, 0, 10, 'ACTIVE', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""".stripMargin
    )

  private def stock(productId: Long): Int =
    TestDatabase.query(s"SELECT stock FROM catalog_product WHERE id = $productId")(_.getInt(1)).head

  private def lastOrderEvent: JsValue =
    Json.parse(
      TestDatabase.query("SELECT payload FROM shared_event WHERE event_type = 'OrderStatusChanged' ORDER BY id DESC LIMIT 1")(_.getString(1)).head
    )

  private def graphql(token: String, query: String): JsValue = {
    val request = FakeRequest(POST, "/graphql").withJsonBody(Json.obj("query" -> query)).withHeaders("Authorization" -> s"Bearer $token")
    val result  = route(app, request).get
    status(result) mustBe OK
    contentAsJson(result)
  }

  "Checkout in GraphQL" must {
    "return the confirmed order for a client" in {
      insertProduct(1)
      graphql(clientToken, "mutation { addToCart(productId: 1, quantity: 2) { total } }")

      val order = graphql(clientToken, "mutation { checkout { id status total items { productId quantity lineTotal } } }") \ "data" \ "checkout"

      UUID.fromString((order \ "id").as[String])
      (order \ "status").as[String] mustBe "CONFIRMED"
      (order \ "total").as[BigDecimal] mustBe BigDecimal("39.98")
      (order \ "items" \ 0 \ "productId").as[Long] mustBe 1L
      (order \ "items" \ 0 \ "quantity").as[Int] mustBe 2
      (order \ "items" \ 0 \ "lineTotal").as[BigDecimal] mustBe BigDecimal("39.98")
    }

    "answer FORBIDDEN to a manager" in {
      val json = graphql(managerToken, "mutation { checkout { id } }")

      (json \ "errors" \ 0 \ "extensions" \ "code").as[String] mustBe "FORBIDDEN"
    }
  }

  "My orders in GraphQL" must {
    "list the client's orders newest first, one page at a time" in {
      insertProduct(1)
      placeOrder(clientToken, 1)
      val second = placeOrder(clientToken, 1)
      val third  = placeOrder(clientToken, 1)
      placeOrder(otherClientToken, 1)

      val page = graphql(clientToken, "query { myOrders(page: 1, size: 2) { totalItems totalPages items { id items { productId quantity } } } }") \ "data" \ "myOrders"

      (page \ "totalItems").as[Long] mustBe 3L
      (page \ "totalPages").as[Long] mustBe 2L
      (page \ "items").as[List[JsValue]].map(o => (o \ "id").as[String]) mustBe List(third, second)
      (page \ "items" \ 0 \ "items" \ 0 \ "productId").as[Long] mustBe 1L
      (page \ "items" \ 1 \ "items" \ 0 \ "quantity").as[Int] mustBe 1
    }

    "return the client's own order" in {
      insertProduct(1)
      val id = placeOrder(clientToken, 1)

      val order = graphql(clientToken, s"""query { order(id: "$id") { id status trackingNumber estimatedDelivery updatedAt items { productId quantity } } }""") \ "data" \ "order"

      (order \ "id").as[String] mustBe id
      (order \ "status").as[String] mustBe "CONFIRMED"
      (order \ "trackingNumber").toOption mustBe Some(JsNull)
      (order \ "estimatedDelivery").toOption mustBe Some(JsNull)
      Instant.parse((order \ "updatedAt").as[String])
      (order \ "items" \ 0 \ "productId").as[Long] mustBe 1L
    }

    "answer NOT_FOUND for another client's order and for an id that is not a UUID" in {
      insertProduct(1)
      val othersId = placeOrder(otherClientToken, 1)

      val others = graphql(clientToken, s"""query { order(id: "$othersId") { id } }""")
      val badId  = graphql(clientToken, """query { order(id: "not-a-uuid") { id } }""")

      (others \ "errors" \ 0 \ "extensions" \ "code").as[String] mustBe "NOT_FOUND"
      (badId \ "errors" \ 0 \ "extensions" \ "code").as[String] mustBe "NOT_FOUND"
    }

    "answer FORBIDDEN to a manager" in {
      val json = graphql(managerToken, "query { myOrders { totalItems } }")

      (json \ "errors" \ 0 \ "extensions" \ "code").as[String] mustBe "FORBIDDEN"
    }
  }

  "Orders in GraphQL" must {
    "list every client's orders newest first for a manager and answer FORBIDDEN to a client" in {
      insertProduct(1)
      val first  = placeOrder(clientToken, 1)
      val second = placeOrder(otherClientToken, 1)

      val page   = graphql(managerToken, "query { orders(page: 1, size: 5) { page size totalItems totalPages items { id } } }") \ "data" \ "orders"
      val client = graphql(clientToken, "query { orders { totalItems } }")

      (page \ "items").as[List[JsValue]].map(o => (o \ "id").as[String]) mustBe List(second, first)
      (page \ "page").as[Int] mustBe 1
      (page \ "size").as[Int] mustBe 5
      (page \ "totalItems").as[Long] mustBe 2L
      (page \ "totalPages").as[Long] mustBe 1L
      (client \ "errors" \ 0 \ "extensions" \ "code").as[String] mustBe "FORBIDDEN"
    }
  }

  "Order status changes in GraphQL" must {
    "ship a confirmed order with its tracking number and estimated delivery" in {
      insertProduct(1)
      val id = placeOrder(clientToken, 1)

      val order = graphql(
        managerToken,
        s"""mutation { shipOrder(id: "$id", trackingNumber: "TRK-123", estimatedDelivery: "2026-10-25") { status trackingNumber estimatedDelivery } }"""
      ) \ "data" \ "shipOrder"

      (order \ "status").as[String] mustBe "SHIPPED"
      (order \ "trackingNumber").as[String] mustBe "TRK-123"
      (order \ "estimatedDelivery").as[String] mustBe "2026-10-25"
      TestDatabase.query("SELECT changed_by_type FROM ordering_order_status_history WHERE new_status = 'SHIPPED'")(_.getString(1)) mustBe List("MANAGER")
      (lastOrderEvent \ "trackingNumber").as[String] mustBe "TRK-123"
    }

    "cancel a confirmed order and give its stock back" in {
      insertProduct(1)
      val id = placeOrder(clientToken, 1)

      val order = graphql(managerToken, s"""mutation { cancelOrder(id: "$id") { status } }""") \ "data" \ "cancelOrder"

      (order \ "status").as[String] mustBe "CANCELLED"
      stock(1) mustBe 10
    }

    "answer CONFLICT for a wrong move, VALIDATION_ERROR for a bad date and FORBIDDEN to a client" in {
      insertProduct(1)
      val id = placeOrder(clientToken, 1)

      val deliver = graphql(managerToken, s"""mutation { deliverOrder(id: "$id") { status } }""")
      val badDate = graphql(managerToken, s"""mutation { shipOrder(id: "$id", trackingNumber: "TRK-123", estimatedDelivery: "tomorrow") { status } }""")
      val client  = graphql(clientToken, s"""mutation { shipOrder(id: "$id", trackingNumber: "TRK-123", estimatedDelivery: "2026-10-25") { status } }""")

      (deliver \ "errors" \ 0 \ "extensions" \ "code").as[String] mustBe "CONFLICT"
      (badDate \ "errors" \ 0 \ "extensions" \ "code").as[String] mustBe "VALIDATION_ERROR"
      (badDate \ "errors" \ 0 \ "extensions" \ "details" \ 0 \ "field").as[String] mustBe "estimatedDelivery"
      (client \ "errors" \ 0 \ "extensions" \ "code").as[String] mustBe "FORBIDDEN"
    }
  }
}
