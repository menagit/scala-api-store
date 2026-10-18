package com.mendev.apistore.ordering.domain

import com.mendev.apistore.shared.error.FieldError

final case class TrackingNumber private (value: String)

object TrackingNumber {
  val MaxLength = 64

  def from(raw: String): Either[FieldError, TrackingNumber] = {
    val trimmed = raw.trim
    if (trimmed.isEmpty) Left(FieldError("trackingNumber", "must not be blank"))
    else if (trimmed.length > MaxLength) Left(FieldError("trackingNumber", s"must be at most $MaxLength characters"))
    else Right(new TrackingNumber(trimmed))
  }
}
