package com.mendev.apistore.shared.error

final case class FieldError(field: String, message: String)

sealed trait AppError {
  def code: String
  def message: String
}

object AppError {

  final case class Validation(message: String, details: List[FieldError] = Nil) extends AppError {
    val code: String = "VALIDATION_ERROR"
  }

  final case class Unauthorized(message: String = "Authentication required") extends AppError {
    val code: String = "UNAUTHORIZED"
  }

  final case class Forbidden(message: String = "Not allowed") extends AppError {
    val code: String = "FORBIDDEN"
  }

  final case class NotFound(message: String) extends AppError {
    val code: String = "NOT_FOUND"
  }

  final case class Conflict(message: String) extends AppError {
    val code: String = "CONFLICT"
  }

  final case class TooManyRequests(message: String = "Too many requests") extends AppError {
    val code: String = "TOO_MANY_REQUESTS"
  }

  final case class Unavailable(message: String = "Service unavailable") extends AppError {
    val code: String = "SERVICE_UNAVAILABLE"
  }

  final case class Unexpected(message: String = "Unexpected error") extends AppError {
    val code: String = "INTERNAL_ERROR"
  }
}