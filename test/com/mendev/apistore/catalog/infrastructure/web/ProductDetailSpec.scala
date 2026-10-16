package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.shared.AppComponentsSuite
import com.mendev.apistore.shared.db.TestDatabase
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class ProductDetailSpec extends PlaySpec with AppComponentsSuite with BeforeAndAfterEach {

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
