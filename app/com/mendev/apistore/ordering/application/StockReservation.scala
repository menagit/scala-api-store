package com.mendev.apistore.ordering.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

trait StockReservation {
  def findProducts(ids: Seq[Long], conn: Connection): Either[AppError, List[CheckoutProduct]]

  def take(productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit]
}
