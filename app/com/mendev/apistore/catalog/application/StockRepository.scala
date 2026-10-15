package com.mendev.apistore.catalog.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

trait StockRepository {
  def decrement(productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit]

  def increment(productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit]
}
