package com.mendev.apistore.ordering.infrastructure.persistence

import com.mendev.apistore.ordering.domain.{ChangedBy, OrderStatusHistoryEntry}
import com.mendev.apistore.ordering.domain.ChangedBy.{Client, Manager, System}

import java.time.Instant
import java.util.UUID

final case class OrderStatusHistoryEntity(
  orderId: Long,
  oldStatus: Option[String],
  newStatus: String,
  changedByType: String,
  changedByPublicId: Option[UUID],
  createdAt: Instant
)

object OrderStatusHistoryEntity {

  def fromDomain(orderId: Long, entry: OrderStatusHistoryEntry): OrderStatusHistoryEntity = {
    val (changedByType, changedByPublicId) = changedByToColumns(entry.changedBy)
    OrderStatusHistoryEntity(
      orderId = orderId,
      oldStatus = entry.oldStatus.map(OrderEntity.statusToText),
      newStatus = OrderEntity.statusToText(entry.newStatus),
      changedByType = changedByType,
      changedByPublicId = changedByPublicId,
      createdAt = entry.createdAt
    )
  }

  private def changedByToColumns(changedBy: ChangedBy): (String, Option[UUID]) = changedBy match {
    case Client(publicId)  => ("CLIENT", Some(publicId))
    case Manager(publicId) => ("MANAGER", Some(publicId))
    case System            => ("SYSTEM", None)
  }
}
