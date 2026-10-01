package controllers

import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import shared.db.TestDatabase

class HealthControllerSpec extends PlaySpec with GuiceOneAppPerSuite {

  implicit lazy val materializer: Materializer = app.materializer

  override def fakeApplication(): Application = {
    val db  = TestDatabase.config
    val url = s"jdbc:mysql://${db.host}:${db.port}/${db.name}"

    new GuiceApplicationBuilder()
      .configure(
        Map(
          "app.database.host"     -> db.host,
          "app.database.port"     -> db.port,
          "app.database.name"     -> db.name,
          "app.database.user"     -> db.user,
          "app.database.password" -> db.password.value,
          "db.default.url"        -> url,
          "db.default.username"   -> db.user,
          "db.default.password"   -> db.password.value,
          "app.mail.host"         -> "localhost",
          "app.mail.port"         -> 1025,
          "app.mail.user"         -> "",
          "app.mail.password"     -> "",
          "app.mail.from"         -> "test@apistore.local"
        )
      )
      .build()
  }

  "GET /health" must {
    "return 200 when MySQL is up" in {
      val result = route(app, FakeRequest(GET, "/health")).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsString(result) must include("\"status\":\"UP\"")
    }
  }

  "an unknown route" must {
    "answer with the JSON error format" in {
      val result = route(app, FakeRequest(GET, "/does-not-exist")).get

      status(result) mustBe NOT_FOUND
      contentAsString(result) must include("\"code\":\"NOT_FOUND\"")
    }
  }
}