package com.mendev.apistore.catalog.domain

import com.mendev.apistore.shared.error.FieldError


final case class Price private (value: BigDecimal)

object Price {
  val Max: BigDecimal = BigDecimal("9999999999.99")

  def from(raw: BigDecimal): Either[FieldError, Price] =
    if (raw <= 0) Left(FieldError("price", "must be above 0"))
    else if (raw > Max) Left(FieldError("price", s"must be at most $Max"))
    else if (raw.bigDecimal.stripTrailingZeros().scale() > 2) Left(FieldError("price", "must have at most 2 decimals"))
    else Right(new Price(raw.setScale(2)))
}