package com.mendev.apistore.identity.infrastructure.web

import com.mendev.apistore.shared.AppComponentsSuite
import org.scalatestplus.play.PlaySpec
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers.*

class PasswordRateLimitSpec extends PlaySpec with AppComponentsSuite {

  private def post(path: String, body: String, ip: String) =
    route(
      app,
      FakeRequest(POST, path, FakeHeaders(Seq(HOST -> "localhost", CONTENT_TYPE -> "application/json")), body, remoteAddress = ip)
    ).get

  "POST /auth/forgot-password" must {
    "answer 429 on the sixth attempt from one address" in {
      (1 to 5).foreach(_ => status(post("/auth/forgot-password", """{"email":"nobody@example.com"}""", "10.0.0.71")) mustBe ACCEPTED)

      status(post("/auth/forgot-password", """{"email":"nobody@example.com"}""", "10.0.0.71")) mustBe TOO_MANY_REQUESTS
    }
  }

  "POST /auth/reset-password" must {
    "answer 429 on the sixth attempt from one address" in {
      (1 to 5).foreach(_ => status(post("/auth/reset-password", "{}", "10.0.0.72")))

      status(post("/auth/reset-password", "{}", "10.0.0.72")) mustBe TOO_MANY_REQUESTS
    }
  }
}
