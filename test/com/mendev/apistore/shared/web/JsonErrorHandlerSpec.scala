package com.mendev.apistore.shared.web

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.HttpEntity
import play.api.mvc.Result
import play.api.test.FakeRequest

import scala.concurrent.Await
import scala.concurrent.duration.*

class JsonErrorHandlerSpec extends AnyWordSpec with Matchers {

  private val handler = new JsonErrorHandler()

  private def bodyOf(result: Result): String = result.body match {
    case strict: HttpEntity.Strict => strict.data.utf8String
    case other                     => fail(s"Expected a strict body but got $other")
  }

  "JsonErrorHandler" should {
    "answer an unknown route with a JSON 404" in {
      val request = FakeRequest("GET", "/does-not-exist")
      val result  = Await.result(handler.onClientError(request, 404, "Not found"), 5.seconds)

      result.header.status shouldBe 404
      bodyOf(result) should include("\"code\":\"NOT_FOUND\"")
    }

    "answer any other client error with a JSON 400" in {
      val request = FakeRequest("POST", "/anything")
      val result  = Await.result(handler.onClientError(request, 422, "Some Play message"), 5.seconds)

      result.header.status shouldBe 400
      bodyOf(result) should include("\"code\":\"VALIDATION_ERROR\"")
      bodyOf(result) should not include "Some Play message"
    }

    "answer an exception with a generic JSON 500 and no internal details" in {
      val request = FakeRequest("GET", "/boom")
      val result  = Await.result(handler.onServerError(request, new RuntimeException("secret detail")), 5.seconds)

      result.header.status shouldBe 500
      bodyOf(result) should include("\"code\":\"INTERNAL_ERROR\"")
      bodyOf(result) should not include "secret detail"
    }
  }
}