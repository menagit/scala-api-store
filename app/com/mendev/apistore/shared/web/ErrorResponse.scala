package com.mendev.apistore.shared.web

import com.mendev.apistore.shared.error.{AppError, FieldError}
import io.circe.Json
import play.api.http.MimeTypes
import play.api.mvc.{Result, Results}

object ErrorResponse {

  def status(error: AppError): Int = error match {
    case _: AppError.Validation      => 400
    case _: AppError.Unauthorized    => 401
    case _: AppError.Forbidden       => 403
    case _: AppError.NotFound        => 404
    case _: AppError.Conflict        => 409
    case _: AppError.TooManyRequests => 429
    case _: AppError.Unavailable     => 503
    case _: AppError.Unexpected      => 500
  }

  def body(error: AppError): Json = {
    val details = error match {
      case AppError.Validation(_, fields) => fields.map(fieldJson)
      case _                              => Nil
    }
    Json.obj(
      "error" -> Json.obj(
        "code"    -> Json.fromString(error.code),
        "message" -> Json.fromString(error.message),
        "details" -> Json.fromValues(details)
      )
    )
  }

  def result(error: AppError): Result =
    Results.Status(status(error))(body(error).noSpaces).as(MimeTypes.JSON)

  private def fieldJson(f: FieldError): Json =
    Json.obj(
      "field"   -> Json.fromString(f.field),
      "message" -> Json.fromString(f.message)
    )
}