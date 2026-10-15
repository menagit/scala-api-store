package com.mendev.apistore.catalog.domain

import com.mendev.apistore.shared.error.FieldError

// 0 ->  no discount.
final case class Discount private (percent: BigDecimal)


object Discount {
  val Max: BigDecimal = BigDecimal("99.99")
  val Zero: Discount  = new Discount(BigDecimal(0).setScale(2))

  def from(raw: BigDecimal): Either[FieldError, Discount] =
    if (raw < 0 || raw > Max) Left(FieldError("discountPercent", s"must be between 0 and $Max"))
    else if (raw.bigDecimal.stripTrailingZeros().scale() > 2) Left(FieldError("discountPercent", "must have at most 2 decimals"))
    else Right(new Discount(raw.setScale(2)))
}