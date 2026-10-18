package com.mendev.apistore.ordering.domain

import com.mendev.apistore.shared.events.DomainEvent

import java.time.Instant
import java.util.UUID

final case class OrderStatusChanged(
  orderPublicId: UUID,
  userPublicId: UUID,
  from: Option[OrderStatus],
  to: OrderStatus,
  changedBy: ChangedBy,
  at: Instant
) extends DomainEvent {

  override def eventType: String = "OrderStatusChanged"

  override def payload: Map[String, String] =
    Map(
      "orderPublicId" -> orderPublicId.toString,
      "userPublicId"  -> userPublicId.toString,
      "to"            -> text(to),
      "changedBy"     -> changedByText,
      "at"            -> at.toString
    ) ++ from.map(status => "from" -> text(status))

  override def toString: String = s"OrderStatusChanged($orderPublicId)"

  private def text(status: OrderStatus): String = status.toString.toUpperCase

  private def changedByText: String = changedBy match {
    case ChangedBy.Client(_)  => "CLIENT"
    case ChangedBy.Manager(_) => "MANAGER"
    case ChangedBy.System     => "SYSTEM"
  }
}

object OrderStatusChanged {
  def of(order: Order, entry: OrderStatusHistoryEntry): OrderStatusChanged =
    OrderStatusChanged(order.publicId, order.userPublicId, entry.oldStatus, entry.newStatus, entry.changedBy, entry.createdAt)
}
