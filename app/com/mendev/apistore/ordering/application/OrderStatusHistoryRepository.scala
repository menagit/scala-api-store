package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.OrderStatusHistoryEntry
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

trait OrderStatusHistoryRepository {
  def insert(orderId: Long, entry: OrderStatusHistoryEntry, conn: Connection): Either[AppError, Unit]
}
