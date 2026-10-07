package com.mendev.apistore.shared.health.web

import com.mendev.apistore.AppComponents
import com.mendev.apistore.shared.db.TestDatabase
import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.components.OneAppPerSuiteWithComponents
import play.api.{ApplicationLoader, BuiltInComponents, Environment}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class HealthControllerSpec extends PlaySpec with OneAppPerSuiteWithComponents {

  implicit lazy val materializer: Materializer = app.materializer

  // Builds the app through our own wiring (AppComponents), the same way AppLoader does in production.
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
        "app.jwt.access-token-ttl" -> "2 minutes"
      )
    )

    new AppComponents(context)
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