package com.mendev.apistore.ordering.domain

import com.mendev.apistore.shared.events.DomainEvent

import java.time.{Instant, LocalDate}
import java.util.UUID

final case class OrderStatusChanged(
  orderPublicId: UUID,
  userPublicId: UUID,
  from: Option[OrderStatus],
  to: OrderStatus,
  changedBy: ChangedBy,
  trackingNumber: Option[TrackingNumber],
  estimatedDelivery: Option[LocalDate],
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
    ) ++ from.map(status => "from" -> text(status)) ++
      trackingNumber.map(t => "trackingNumber" -> t.value) ++
      estimatedDelivery.map(d => "estimatedDelivery" -> d.toString)

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
    OrderStatusChanged(
      order.publicId,
      order.userPublicId,
      entry.oldStatus,
      entry.newStatus,
      entry.changedBy,
      order.trackingNumber,
      order.estimatedDelivery,
      entry.createdAt
    )
}
