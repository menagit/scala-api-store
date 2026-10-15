package com.mendev.apistore.catalog.domain

import com.mendev.apistore.shared.error.FieldError

final case class Stock private (value: Int)

object Stock {
  def from(raw: Int): Either[FieldError, Stock] =
    if (raw < 0) Left(FieldError("stock", "must be 0 or more"))
    else Right(new Stock(raw))
}