package com.mendev.apistore.shared.web

import com.mendev.apistore.shared.error.{AppError, FieldError}
import play.api.libs.json.{JsObject, JsValue, Json}
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

  def body(error: AppError): JsValue = {
    val details: Seq[JsObject] = error match {
      case AppError.Validation(_, fields) => fields.map(fieldJson)
      case _                              => Nil
    }
    Json.obj(
      "error" -> Json.obj(
        "code"    -> error.code,
        "message" -> error.message,
        "details" -> details
      )
    )
  }

  def result(error: AppError): Result =
    Results.Status(status(error))(body(error))

  private def fieldJson(f: FieldError): JsObject =
    Json.obj("field" -> f.field, "message" -> f.message)
}