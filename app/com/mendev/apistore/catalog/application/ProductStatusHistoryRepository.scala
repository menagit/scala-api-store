package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.ProductStatusHistoryEntry
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

trait ProductStatusHistoryRepository {
  def insert(entry: ProductStatusHistoryEntry, conn: Connection): Either[AppError, Unit]

  def findByProductId(productId: Long, conn: Connection): Either[AppError, List[ProductStatusHistoryEntry]]
}
