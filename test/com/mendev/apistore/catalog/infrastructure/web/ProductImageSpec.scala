package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.shared.AppComponentsSuite
import com.mendev.apistore.shared.db.TestDatabase
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{MultipartFormData, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.awt.image.BufferedImage
import java.net.URI
import java.nio.file.{Files, Path}
import javax.imageio.ImageIO
import scala.concurrent.Future

class ProductImageSpec extends PlaySpec with AppComponentsSuite with BeforeAndAfterEach {

  private lazy val storageDir: Path = Files.createTempDirectory("product-images")

  override def extraSettings: Map[String, String] = Map(
    "app.images.storage-dir"     -> storageDir.toString,
    "app.images.max-per-product" -> "1"
  )

  override def beforeEach(): Unit =
    List("catalog_product_image", "catalog_product").foreach(table => TestDatabase.update(s"DELETE FROM $table"))

  private val password = "Secret1234"

  private lazy val clientToken: String = {
    signUp("image-spec-client@apistore.com")
    signIn("image-spec-client@apistore.com")
  }

  private lazy val managerToken: String = {
    signUp("image-spec-manager@apistore.com")
    TestDatabase.update("UPDATE identity_user SET role = 'MANAGER' WHERE email = 'image-spec-manager@apistore.com'")
    signIn("image-spec-manager@apistore.com")
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
         |VALUES ($id, 'Mouse', 'Wireless mouse', 1, 19.99, 0, 5, 'ACTIVE', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""".stripMargin
    )

  private def image(width: Int, height: Int, format: String): Path = {
    val file = Files.createTempFile("upload", s".$format")
    ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, file.toFile)
    file
  }

  private def upload(productId: Long, file: Path, token: String): Future[Result] = {
    val part = MultipartFormData.FilePart("file", "image", None, SingletonTemporaryFileCreator.create(file))
    val form = MultipartFormData(Map.empty[String, Seq[String]], Seq(part), Nil)
    val request = FakeRequest(POST, s"/products/$productId/images")
      .withMultipartFormDataBody(form)
      .withHeaders("Authorization" -> s"Bearer $token")
    route(app, request).get
  }

  private def get(path: String): Future[Result] = route(app, FakeRequest(GET, path)).get

  private def stored(url: String): Path = storageDir.resolve(new URI(url).getPath.stripPrefix("/images/"))

  private def size(url: String): (Int, Int) = {
    val read = ImageIO.read(stored(url).toFile)
    (read.getWidth, read.getHeight)
  }

  "POST /products/:id/images" must {
    "store a large copy and a thumbnail and show the image in the product responses" in {
      insertProduct(1)

      val result = upload(1, image(2000, 1500, "png"), managerToken)

      status(result) mustBe CREATED
      val json     = contentAsJson(result)
      val largeUrl = (json \ "largeUrl").as[String]
      val thumbUrl = (json \ "thumbUrl").as[String]
      (json \ "id").asOpt[Long] must not be empty
      (json \ "position").as[Int] mustBe 1
      size(largeUrl) mustBe ((1200, 900))
      size(thumbUrl) mustBe ((400, 300))

      (contentAsJson(get("/products/1")) \ "images").as[List[JsValue]] mustBe List(json)
      (contentAsJson(get("/products")) \ "items" \ 0 \ "coverImage").as[JsObject] mustBe
        Json.obj("largeUrl" -> largeUrl, "thumbUrl" -> thumbUrl)

      val served = get(new URI(thumbUrl).getPath)
      status(served) mustBe OK
      contentType(served) mustBe Some("image/png")
    }

    "refuse a GIF with 400 on file, a client with 403 and an image over the limit with 409" in {
      insertProduct(2)
      val png = image(2000, 1500, "png")

      val gif = upload(2, image(10, 10, "gif"), managerToken)
      status(gif) mustBe BAD_REQUEST
      (contentAsJson(gif) \ "error" \ "details" \ 0 \ "field").as[String] mustBe "file"

      status(upload(2, png, clientToken)) mustBe FORBIDDEN

      status(upload(2, png, managerToken)) mustBe CREATED
      val full = upload(2, png, managerToken)
      status(full) mustBe CONFLICT
      (contentAsJson(full) \ "error" \ "message").as[String] mustBe "Product already has 1 images"
    }
  }

  "DELETE /products/:id/images/:imageId" must {
    "remove the row and both files" in {
      insertProduct(3)
      val json     = contentAsJson(upload(3, image(2000, 1500, "png"), managerToken))
      val largeUrl = (json \ "largeUrl").as[String]
      val thumbUrl = (json \ "thumbUrl").as[String]

      val request = FakeRequest(DELETE, s"/products/3/images/${(json \ "id").as[Long]}")
        .withHeaders("Authorization" -> s"Bearer $managerToken")
      status(route(app, request).get) mustBe NO_CONTENT

      Files.exists(stored(largeUrl)) mustBe false
      Files.exists(stored(thumbUrl)) mustBe false
      (contentAsJson(get("/products/3")) \ "images").as[List[JsValue]] mustBe Nil
    }
  }
}
