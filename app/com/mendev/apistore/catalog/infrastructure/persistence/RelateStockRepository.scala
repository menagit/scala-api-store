package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.catalog.application.StockRepository
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

class RelateStockRepository extends StockRepository {

  override def decrement(productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit] = {
    val updated = sql"""
      UPDATE catalog_product
      SET stock = stock - $quantity, updated_at = $now
      WHERE id = $productId AND stock >= $quantity AND status = 'ACTIVE' AND deleted_at IS NULL
    """.executeUpdate()(conn)

    updated match {
      case 1 => Right(())
      case _ => Left(AppError.Conflict("Product unavailable or insufficient stock"))
    }
  }

  override def increment(productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit] = {
    val updated = sql"""
      UPDATE catalog_product
      SET stock = stock + $quantity, updated_at = $now
      WHERE id = $productId
    """.executeUpdate()(conn)

    updated match {
      case 1 => Right(())
      case _ => Left(AppError.NotFound("Product not found"))
    }
  }
}
