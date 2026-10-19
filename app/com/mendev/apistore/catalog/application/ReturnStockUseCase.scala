package com.mendev.apistore.catalog.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

class ReturnStockUseCase(stockRepo: StockRepository) {

  def executeIn(productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit] =
    stockRepo.increment(productId, quantity, now, conn)
}
