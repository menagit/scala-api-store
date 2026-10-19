package com.mendev.apistore.graphql

import com.mendev.apistore.shared.AppComponentsSuite
import com.mendev.apistore.shared.db.TestDatabase
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class CatalogSchemaSpec extends PlaySpec with AppComponentsSuite with BeforeAndAfterEach {

  override def beforeEach(): Unit =
    List(
      "catalog_product_status_history",
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
    signUp("catalog-client@apistore.com")
    signIn("catalog-client@apistore.com")
  }

  private lazy val managerToken: String = {
    signUp("catalog-manager@apistore.com")
    TestDatabase.update("UPDATE identity_user SET role = 'MANAGER' WHERE email = 'catalog-manager@apistore.com'")
    signIn("catalog-manager@apistore.com")
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

  private def publicIds: List[Long] =
    (contentAsJson(route(app, FakeRequest(GET, "/products")).get) \ "items").as[List[JsValue]].map(p => (p \ "id").as[Long])

  private def history(productId: Long): List[(String, String, String)] =
    TestDatabase
      .query(s"SELECT action, old_status, new_status FROM catalog_product_status_history WHERE product_id = $productId ORDER BY id")(r =>
        (r.getString(1), r.getString(2), r.getString(3))
      )
      .toList

  private def isDeleted(productId: Long): Boolean =
    TestDatabase.query(s"SELECT deleted_at IS NOT NULL FROM catalog_product WHERE id = $productId")(_.getBoolean(1)).head

  private def errorCode(json: JsValue): String = (json \ "errors" \ 0 \ "extensions" \ "code").as[String]

  "Product mutations in GraphQL" must {
    "disable a product: it leaves the public reads and one STATUS_CHANGED row is written" in {
      insertProduct(1)

      val product = graphql(managerToken, "mutation { disableProduct(id: 1) { id status deleted } }") \ "data" \ "disableProduct"

      (product \ "status").as[String] mustBe "DISABLED"
      (product \ "deleted").as[Boolean] mustBe false
      publicIds mustBe empty
      status(route(app, FakeRequest(GET, "/products/1")).get) mustBe NOT_FOUND
      history(1) mustBe List(("STATUS_CHANGED", "ACTIVE", "DISABLED"))
    }

    "enable a disabled product: it is back in the public list" in {
      insertProduct(1)
      graphql(managerToken, "mutation { disableProduct(id: 1) { id } }")

      val product = graphql(managerToken, "mutation { enableProduct(id: 1) { status } }") \ "data" \ "enableProduct"

      (product \ "status").as[String] mustBe "ACTIVE"
      publicIds mustBe List(1L)
    }

    "write one history row when disableProduct is called twice" in {
      insertProduct(1)
      graphql(managerToken, "mutation { disableProduct(id: 1) { id } }")

      val product = graphql(managerToken, "mutation { disableProduct(id: 1) { status } }") \ "data" \ "disableProduct"

      (product \ "status").as[String] mustBe "DISABLED"
      history(1).size mustBe 1
    }

    "answer FORBIDDEN to a client on disableProduct and deleteProduct" in {
      insertProduct(1)

      errorCode(graphql(clientToken, "mutation { disableProduct(id: 1) { id } }")) mustBe "FORBIDDEN"
      errorCode(graphql(clientToken, "mutation { deleteProduct(id: 1) { id } }")) mustBe "FORBIDDEN"
      history(1) mustBe empty
    }

    "delete a product: deleted_at is set, one DELETED row is written and it leaves the public list" in {
      insertProduct(1)

      val product = graphql(managerToken, "mutation { deleteProduct(id: 1) { id deleted } }") \ "data" \ "deleteProduct"

      (product \ "deleted").as[Boolean] mustBe true
      isDeleted(1) mustBe true
      history(1) mustBe List(("DELETED", "ACTIVE", "ACTIVE"))
      publicIds mustBe empty
    }

    "answer CONFLICT on deleteProduct while the product is in a confirmed order" in {
      insertProduct(1)
      placeOrder(clientToken, 1)

      val json = graphql(managerToken, "mutation { deleteProduct(id: 1) { id } }")

      errorCode(json) mustBe "CONFLICT"
      (json \ "errors" \ 0 \ "message").as[String] mustBe "Product has pending orders"
      isDeleted(1) mustBe false
      history(1) mustBe empty
    }

    "delete the product once its order is cancelled" in {
      insertProduct(1)
      val orderId = placeOrder(clientToken, 1)
      graphql(managerToken, s"""mutation { cancelOrder(id: "$orderId") { status } }""")

      val product = graphql(managerToken, "mutation { deleteProduct(id: 1) { deleted } }") \ "data" \ "deleteProduct"

      (product \ "deleted").as[Boolean] mustBe true
    }

    "answer NOT_FOUND on all three mutations after a delete" in {
      insertProduct(1)
      graphql(managerToken, "mutation { deleteProduct(id: 1) { id } }")

      List("disableProduct", "enableProduct", "deleteProduct").foreach { mutation =>
        errorCode(graphql(managerToken, s"mutation { $mutation(id: 1) { id } }")) mustBe "NOT_FOUND"
      }
    }
  }
}
