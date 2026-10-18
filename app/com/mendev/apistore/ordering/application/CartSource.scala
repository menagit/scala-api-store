package com.mendev.apistore.ordering.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant
import java.util.UUID

trait CartSource {
  def lockItems(userPublicId: UUID, now: Instant, conn: Connection): Either[AppError, List[CheckoutLine]]

  def clear(userPublicId: UUID, conn: Connection): Either[AppError, Unit]
}
