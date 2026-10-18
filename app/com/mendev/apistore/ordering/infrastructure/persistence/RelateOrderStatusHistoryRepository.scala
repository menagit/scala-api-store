package com.mendev.apistore.ordering.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.ordering.application.OrderStatusHistoryRepository
import com.mendev.apistore.ordering.domain.OrderStatusHistoryEntry
import com.mendev.apistore.shared.db.UuidBytes
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class RelateOrderStatusHistoryRepository extends OrderStatusHistoryRepository {

  override def insert(orderId: Long, entry: OrderStatusHistoryEntry, conn: Connection): Either[AppError, Unit] = {
    val entity = OrderStatusHistoryEntity.fromDomain(orderId, entry)
    sql"""
      INSERT INTO ordering_order_status_history
        (order_id, old_status, new_status, changed_by_type, changed_by_public_id, created_at)
      VALUES
        (${entity.orderId}, ${entity.oldStatus}, ${entity.newStatus}, ${entity.changedByType},
         ${entity.changedByPublicId.map(UuidBytes.toBytes)}, ${entity.createdAt})
    """.executeUpdate()(conn)
    Right(())
  }
}
