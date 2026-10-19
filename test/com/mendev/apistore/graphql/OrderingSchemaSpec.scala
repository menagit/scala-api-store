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
}
