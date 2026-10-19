package com.mendev.apistore.shared.web

import com.mendev.apistore.shared.AppComponentsSuite
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class CorsSpec extends PlaySpec with AppComponentsSuite {

  override def extraSettings: Map[String, String] = Map("app.cors.allowed-origins" -> "http://localhost:8000")

  private def preflight(origin: String) =
    route(app, FakeRequest(OPTIONS, "/graphql").withHeaders(ORIGIN -> origin, ACCESS_CONTROL_REQUEST_METHOD -> POST)).get

  "A CORS preflight" must {
    "be allowed from a configured origin" in {
      val result = preflight("http://localhost:8000")

      status(result) mustBe OK
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) mustBe Some("http://localhost:8000")
    }

    "be refused from any other origin" in {
      val result = preflight("http://evil.example")

      status(result) mustBe FORBIDDEN
      header(ACCESS_CONTROL_ALLOW_ORIGIN, result) mustBe None
    }
  }
}
