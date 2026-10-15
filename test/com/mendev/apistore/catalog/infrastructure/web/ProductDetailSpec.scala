package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.AppComponents
import com.mendev.apistore.shared.db.TestDatabase
import org.apache.pekko.stream.Materializer
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.components.OneAppPerSuiteWithComponents
import play.api.{ApplicationLoader, BuiltInComponents, Environment}
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class ProductDetailSpec extends PlaySpec with OneAppPerSuiteWithComponents with BeforeAndAfterEach {

  implicit lazy val materializer: Materializer = app.materializer

  override def components: BuiltInComponents = {
    val db  = TestDatabase.config
    val url = s"jdbc:mysql://${db.host}:${db.port}/${db.name}"

    val context = ApplicationLoader.Context.create(
      Environment.simple(),
      initialSettings = Map(
        "app.database.host"     -> db.host,
        "app.database.port"     -> db.port.toString,
        "app.database.name"     -> db.name,
        "app.database.user"     -> db.user,
        "app.database.password" -> db.password.value,
        "db.default.url"        -> url,
        "db.default.username"   -> db.user,
        "db.default.password"   -> db.password.value,
        "app.mail.host"         -> "localhost",
        "app.mail.port"         -> "1025",
        "app.mail.user"         -> "",
        "app.mail.password"     -> "",
        "app.mail.from"         -> "test@apistore.local",
        "app.jwt.secret"        -> "test-secret-test-secret-test-secret-1234",
        "app.jwt.access-token-ttl" -> "2 minutes",
        "app.encryption.key"       -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.mail.link-base-url"   -> "http://localhost:3000",
      )
    )

    new AppComponents(context)
  }

  override def beforeEach(): Unit = TestDatabase.update("DELETE FROM catalog_product")

  private val notFoundBody =
    Json.obj("error" -> Json.obj("code" -> "NOT_FOUND", "message" -> "Product not found", "details" -> Json.arr()))

  private def insertProduct(price: String, discount: String, stock: Int, status: String, deletedAt: String = "NULL"): Unit =
    TestDatabase.update(
      s"""INSERT INTO catalog_product
         |(id, name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at)
         |VALUES (1, 'Mouse', 'Wireless mouse', 1, $price, $discount, $stock, '$status', $deletedAt, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""".stripMargin
    )

  private def get(path: String) = route(app, FakeRequest(GET, path)).get

  "GET /products/:id" must {
    "return an active product with no token" in {
      insertProduct("19.99", "0", 5, "ACTIVE")

      val result = get("/products/1")

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj(
        "id"              -> 1,
        "name"            -> "Mouse",
        "description"     -> "Wireless mouse",
        "category"        -> Json.obj("id" -> 1, "name" -> "Electronics"),
        "price"           -> BigDecimal("19.99"),
        "discountPercent" -> BigDecimal("0"),
        "discountedPrice" -> BigDecimal("19.99"),
        "stock"           -> 5,
        "inStock"         -> true,
        "images"          -> Json.arr()
      )
      contentAsJson(result).as[JsObject].keys must contain noneOf ("status", "createdAt", "updatedAt", "deletedAt")
    }

    "show stock 0 as not in stock" in {
      insertProduct("19.99", "0", 0, "ACTIVE")

      val json = contentAsJson(get("/products/1"))

      (json \ "stock").as[Int] mustBe 0
      (json \ "inStock").as[Boolean] mustBe false
    }

    "apply the discount rule" in {
      insertProduct("19.99", "10", 5, "ACTIVE")

      (contentAsJson(get("/products/1")) \ "discountedPrice").as[BigDecimal] mustBe BigDecimal("17.99")
    }

    "return 404 for a disabled product" in {
      insertProduct("19.99", "0", 5, "DISABLED")

      val result = get("/products/1")

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe notFoundBody
    }

    "return 404 for a soft-deleted product" in {
      insertProduct("19.99", "0", 5, "ACTIVE", deletedAt = "UTC_TIMESTAMP(6)")

      val result = get("/products/1")

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe notFoundBody
    }

    "return 404 for an unknown id" in {
      val result = get("/products/999999")

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe notFoundBody
    }
  }
}
