package com.mendev.apistore.ordering.domain

import java.time.Instant
import java.util.UUID

final case class OrderStatusHistoryEntry private (
  oldStatus: Option[OrderStatus],
  newStatus: OrderStatus,
  changedBy: ChangedBy,
  createdAt: Instant
)

object OrderStatusHistoryEntry {

  def created(userPublicId: UUID, now: Instant): OrderStatusHistoryEntry =
    new OrderStatusHistoryEntry(None, OrderStatus.Pending, ChangedBy.Client(userPublicId), now)

  def changed(from: OrderStatus, to: OrderStatus, by: ChangedBy, now: Instant): OrderStatusHistoryEntry =
    new OrderStatusHistoryEntry(Some(from), to, by, now)
}
