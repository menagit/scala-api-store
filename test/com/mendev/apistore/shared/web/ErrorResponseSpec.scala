package com.mendev.apistore.shared.web

import com.mendev.apistore.shared.error.{AppError, FieldError}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ErrorResponseSpec extends AnyWordSpec with Matchers {

  "ErrorResponse.status" should {
    "map every error to its HTTP status" in {
      ErrorResponse.status(AppError.Validation("bad")) shouldBe 400
      ErrorResponse.status(AppError.Unauthorized()) shouldBe 401
      ErrorResponse.status(AppError.Forbidden()) shouldBe 403
      ErrorResponse.status(AppError.NotFound("x")) shouldBe 404
      ErrorResponse.status(AppError.Conflict("x")) shouldBe 409
      ErrorResponse.status(AppError.TooManyRequests()) shouldBe 429
      ErrorResponse.status(AppError.Unavailable()) shouldBe 503
      ErrorResponse.status(AppError.Unexpected()) shouldBe 500
    }
  }

  "ErrorResponse.body" should {
    "write code, message and an empty details list" in {
      val error = ErrorResponse.body(AppError.NotFound("Product not found")) \ "error"

      (error \ "code").as[String] shouldBe "NOT_FOUND"
      (error \ "message").as[String] shouldBe "Product not found"
      (error \ "details").as[List[String]] shouldBe Nil
    }

    "write the field list for a validation error" in {
      val fields = List(FieldError("email", "must be a valid email"))
      val error = ErrorResponse.body(AppError.Validation("Invalid input", fields)) \ "error"
      val first = (error \ "details")(0)

      (error \ "code").as[String] shouldBe "VALIDATION_ERROR"
      (first \ "field").as[String] shouldBe "email"
      (first \ "message").as[String] shouldBe "must be a valid email"
    }
  }

  "ErrorResponse.result" should {
    "return the status and a JSON content type" in {
      val result = ErrorResponse.result(AppError.Conflict("Email already exists"))

      result.header.status shouldBe 409
      result.body.contentType shouldBe Some("application/json")
    }
  }
}