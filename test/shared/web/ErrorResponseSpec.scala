package shared.web

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import shared.error.{AppError, FieldError}

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
      val error = ErrorResponse.body(AppError.NotFound("Product not found")).hcursor.downField("error")

      error.downField("code").as[String] shouldBe Right("NOT_FOUND")
      error.downField("message").as[String] shouldBe Right("Product not found")
      error.downField("details").as[List[String]] shouldBe Right(Nil)
    }

    "write the field list for a validation error" in {
      val fields = List(FieldError("email", "must be a valid email"))
      val error = ErrorResponse.body(AppError.Validation("Invalid input", fields)).hcursor.downField("error")
      val first = error.downField("details").downArray

      error.downField("code").as[String] shouldBe Right("VALIDATION_ERROR")
      first.downField("field").as[String] shouldBe Right("email")
      first.downField("message").as[String] shouldBe Right("must be a valid email")
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