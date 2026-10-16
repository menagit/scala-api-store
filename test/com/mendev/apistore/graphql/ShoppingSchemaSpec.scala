package com.mendev.apistore.graphql

import com.mendev.apistore.shared.AppComponentsSuite
import com.mendev.apistore.shared.db.TestDatabase
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class ShoppingSchemaSpec extends PlaySpec with AppComponentsSuite with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    TestDatabase.update("DELETE FROM shopping_cart_item")
    TestDatabase.update("DELETE FROM shopping_cart")
    TestDatabase.update("DELETE FROM catalog_product")
  }

  private val password = "Secret1234"

  private lazy val clientToken: String = {
    signUp("cart-client@apistore.com")
    signIn("cart-client@apistore.com")
  }

  private lazy val managerToken: String = {
    signUp("cart-manager@apistore.com")
    TestDatabase.update("UPDATE identity_user SET role = 'MANAGER' WHERE email = 'cart-manager@apistore.com'")
    signIn("cart-manager@apistore.com")
  }

  private def signUp(email: String): Unit = {
    val body = Json.obj("email" -> email, "password" -> password, "firstName" -> "Ana", "lastName" -> "Ruiz")
    status(route(app, FakeRequest(POST, "/auth/sign-up").withJsonBody(body)).get) mustBe CREATED
  }

  private def signIn(email: String): String = {
    val body = Json.obj("email" -> email, "password" -> password)
    (contentAsJson(route(app, FakeRequest(POST, "/auth/sign-in").withJsonBody(body)).get) \ "access_token").as[String]
  }

  private def insertProduct(id: Long, status: String): Unit =
    TestDatabase.update(
      s"""INSERT INTO catalog_product
         |(id, name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at)
         |VALUES ($id, 'Mouse', 'Wireless mouse', 1, 19.99, 0, 10, '$status', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""".stripMargin
    )

  private def graphql(token: String, query: String): JsValue = {
    val request = FakeRequest(POST, "/graphql").withJsonBody(Json.obj("query" -> query)).withHeaders("Authorization" -> s"Bearer $token")
    val result  = route(app, request).get
    status(result) mustBe OK
    contentAsJson(result)
  }

  "The cart in GraphQL" must {
    "add a line and return the updated cart" in {
      insertProduct(1, "ACTIVE")

      val cart = graphql(clientToken, "mutation { addToCart(productId: 1, quantity: 2) { lines { productId quantity lineTotal } total } }") \ "data" \ "addToCart"

      (cart \ "lines" \ 0 \ "productId").as[Long] mustBe 1L
      (cart \ "lines" \ 0 \ "quantity").as[Int] mustBe 2
      (cart \ "lines" \ 0 \ "lineTotal").as[BigDecimal] mustBe BigDecimal("39.98")
      (cart \ "total").as[BigDecimal] mustBe BigDecimal("39.98")
    }

    "update a line's quantity and return the updated cart" in {
      insertProduct(1, "ACTIVE")
      graphql(clientToken, "mutation { addToCart(productId: 1, quantity: 1) { total } }")

      val cart = graphql(clientToken, "mutation { updateCartItemQuantity(productId: 1, quantity: 3) { lines { quantity } } }") \ "data" \ "updateCartItemQuantity"

      (cart \ "lines" \ 0 \ "quantity").as[Int] mustBe 3
    }

    "answer FORBIDDEN to a manager" in {
      insertProduct(1, "ACTIVE")

      val json = graphql(managerToken, "mutation { addToCart(productId: 1, quantity: 1) { total } }")

      (json \ "errors" \ 0 \ "extensions" \ "code").as[String] mustBe "FORBIDDEN"
    }

    "answer NOT_FOUND for an unavailable product" in {
      insertProduct(1, "DISABLED")

      val error = graphql(clientToken, "mutation { addToCart(productId: 1, quantity: 1) { total } }") \ "errors" \ 0

      (error \ "extensions" \ "code").as[String] mustBe "NOT_FOUND"
      (error \ "message").as[String] mustBe "Product not found"
    }

    "return an empty cart for a client with no cart" in {
      val cart = graphql(clientToken, "{ myCart { lines { productId } total } }") \ "data" \ "myCart"

      (cart \ "lines").as[List[JsValue]] mustBe empty
      (cart \ "total").as[BigDecimal] mustBe BigDecimal(0)
    }
  }
}
