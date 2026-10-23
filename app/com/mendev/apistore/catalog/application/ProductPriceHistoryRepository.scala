package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.ProductPriceChange
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

trait ProductPriceHistoryRepository {
  def insert(change: ProductPriceChange, conn: Connection): Either[AppError, Unit]
}
