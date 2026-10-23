package com.mendev.apistore.catalog.domain

import com.mendev.apistore.shared.error.FieldError

object ProductRules {
  val MaxNameLength: Int        = 200
  val MaxDescriptionLength: Int = 2000

  def name(raw: String): Either[FieldError, String] = {
    val clean = raw.strip()
    if (clean.isEmpty || clean.codePointCount(0, clean.length) > MaxNameLength)
      Left(FieldError("name", s"must be between 1 and $MaxNameLength characters"))
    else Right(clean)
  }

  def description(raw: String): Either[FieldError, String] = {
    val clean = raw.strip()
    if (clean.codePointCount(0, clean.length) > MaxDescriptionLength)
      Left(FieldError("description", s"must be at most $MaxDescriptionLength characters"))
    else Right(clean)
  }
}
