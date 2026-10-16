package com.mendev.apistore.shared.health.web

import com.mendev.apistore.shared.AppComponentsSuite
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class HealthControllerSpec extends PlaySpec with AppComponentsSuite {

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