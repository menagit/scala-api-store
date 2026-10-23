package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.shared.AppComponentsSuite
import com.mendev.apistore.shared.db.TestDatabase
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.Future

class ManagerProductSpec extends PlaySpec with AppComponentsSuite with BeforeAndAfterEach {

  override def beforeEach(): Unit =
    List("catalog_product_price_history", "catalog_product_status_history", "shared_event", "catalog_product")
      .foreach(table => TestDatabase.update(s"DELETE FROM $table"))

  private val password = "Secret1234"

  private lazy val clientToken: String = {
    signUp("manager-spec-client@apistore.com")
    signIn("manager-spec-client@apistore.com")
  }

  private lazy val managerToken: String = {
    signUp("manager-spec-manager@apistore.com")
    TestDatabase.update("UPDATE identity_user SET role = 'MANAGER' WHERE email = 'manager-spec-manager@apistore.com'")
    signIn("manager-spec-manager@apistore.com")
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
         |VALUES ($id, 'Mouse', 'Wireless mouse', 1, 19.99, 0, 5, '$status', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""".stripMargin
    )

  private def get(path: String, token: Option[String]): Future[Result] = {
    val request = FakeRequest(GET, path)
    route(app, token.fold(request)(t => request.withHeaders("Authorization" -> s"Bearer $t"))).get
  }

  private def patch(id: Long, body: JsValue, token: String): Future[Result] =
    route(app, FakeRequest(PATCH, s"/products/$id").withJsonBody(body).withHeaders("Authorization" -> s"Bearer $token")).get

  private def count(sql: String): Int = TestDatabase.query(sql)(_.getInt(1)).head

  private def errorFields(result: Future[Result]): List[String] =
    (contentAsJson(result) \ "error" \ "details").as[List[JsValue]].map(d => (d \ "field").as[String])

  "GET /manager/products" must {
    "show active and disabled products to a manager, 403 to a client and 401 without a token" in {
      insertProduct(1, "ACTIVE")
      insertProduct(2, "DISABLED")

      val result = get("/manager/products", Some(managerToken))

      status(result) mustBe OK
      val items = (contentAsJson(result) \ "items").as[List[JsValue]]
      items.map(i => (i \ "id").as[Long] -> (i \ "status").as[String]) must contain theSameElementsAs List(1L -> "ACTIVE", 2L -> "DISABLED")
      status(get("/manager/products", Some(clientToken))) mustBe FORBIDDEN
      status(get("/manager/products", None)) mustBe UNAUTHORIZED
    }
  }

  "PATCH /products/:id" must {
    "change only the sent fields, record price changes and write an event for a discount change" in {
      insertProduct(1, "ACTIVE")

      val priceResult = patch(1, Json.obj("price" -> BigDecimal("12.50")), managerToken)

      status(priceResult) mustBe OK
      val json = contentAsJson(priceResult)
      (json \ "price").as[BigDecimal] mustBe BigDecimal("12.50")
      (json \ "name").as[String] mustBe "Mouse"
      (json \ "stock").as[Int] mustBe 5
      TestDatabase.query("SELECT old_price, new_price FROM catalog_product_price_history WHERE product_id = 1")(r =>
        (BigDecimal(r.getBigDecimal(1)), BigDecimal(r.getBigDecimal(2)))
      ).toList mustBe List(BigDecimal("19.99") -> BigDecimal("12.50"))

      status(patch(1, Json.obj("discountPercent" -> 20), managerToken)) mustBe OK
      count("SELECT COUNT(*) FROM shared_event WHERE event_type = 'ProductDiscountChanged'") mustBe 1

      status(patch(1, Json.obj(), managerToken)) mustBe OK
      count("SELECT COUNT(*) FROM catalog_product_price_history") mustBe 2
      count("SELECT COUNT(*) FROM shared_event WHERE event_type = 'ProductDiscountChanged'") mustBe 1
    }

    "reject bad values with field errors, a client with 403 and a missing id with 404" in {
      insertProduct(1, "ACTIVE")

      val nullName = patch(1, Json.obj("name" -> JsNull), managerToken)
      status(nullName) mustBe BAD_REQUEST
      errorFields(nullName) mustBe List("name")

      val badValues = patch(1, Json.obj("stock" -> -1, "discountPercent" -> 100), managerToken)
      status(badValues) mustBe BAD_REQUEST
      errorFields(badValues) must contain theSameElementsAs List("stock", "discountPercent")

      status(patch(1, Json.obj("price" -> 10), clientToken)) mustBe FORBIDDEN
      status(patch(999999, Json.obj("price" -> 10), managerToken)) mustBe NOT_FOUND
    }
  }
}
