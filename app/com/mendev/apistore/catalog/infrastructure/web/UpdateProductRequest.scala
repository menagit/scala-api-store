package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.catalog.application.UpdateProductCommand
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.web.JsonBody
import play.api.libs.json.{JsNull, JsObject, Reads}

object UpdateProductRequest {

  def read(body: String): Either[AppError, UpdateProductCommand] =
    JsonBody.read[JsObject](body).flatMap { json =>
      val name            = field[String](json, "name")
      val description     = field[String](json, "description")
      val categoryId      = field[Long](json, "categoryId")
      val price           = field[BigDecimal](json, "price")
      val discountPercent = field[BigDecimal](json, "discountPercent")
      val stock           = field[Int](json, "stock")
      (name, description, categoryId, price, discountPercent, stock) match {
        case (Right(n), Right(d), Right(c), Right(p), Right(dp), Right(s)) =>
          Right(UpdateProductCommand(n, d, c, p, dp, s))
        case _ =>
          val errors = List(name, description, categoryId, price, discountPercent, stock).collect { case Left(e) => e }
          Left(AppError.Validation("Invalid request", errors))
      }
    }

  // Absent: None. null: an error. A value of the wrong type: an error.
  private def field[A: Reads](json: JsObject, key: String): Either[FieldError, Option[A]] =
    json.value.get(key) match {
      case None         => Right(None)
      case Some(JsNull) => Left(FieldError(key, "must not be null"))
      case Some(value)  => value.asOpt[A].map(Some(_)).toRight(FieldError(key, "is not valid"))
    }
}
