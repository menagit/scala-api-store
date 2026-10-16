package com.mendev.apistore.shared.web

import com.mendev.apistore.shared.error.{AppError, FieldError}
import play.api.libs.json.{Json, Reads}

import scala.util.Try

object JsonBody {

  private val invalidBody: AppError =
    AppError.Validation("Invalid request", List(FieldError("body", "is not valid for this endpoint")))

  def read[A: Reads](body: String): Either[AppError, A] =
    Try(Json.parse(body)).toOption.flatMap(_.asOpt[A]).toRight(invalidBody)
}
