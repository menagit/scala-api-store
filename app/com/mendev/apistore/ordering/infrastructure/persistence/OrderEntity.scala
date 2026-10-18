package com.mendev.apistore.ordering.infrastructure.persistence

import com.lucidchart.relate.SqlRow
import com.mendev.apistore.ordering.domain.{Order, OrderItem, OrderStatus, TrackingNumber}
import com.mendev.apistore.ordering.domain.OrderStatus.{Cancelled, Confirmed, Delivered, Pending, Shipped}
import com.mendev.apistore.shared.db.UuidBytes

import java.time.{Instant, LocalDate}
import java.util.UUID

final case class OrderEntity(
  id: Long,
  publicId: UUID,
  userPublicId: UUID,
  status: String,
  total: BigDecimal,
  trackingNumber: Option[String],
  estimatedDelivery: Option[LocalDate],
  createdAt: Instant,
  updatedAt: Instant
)

object OrderEntity {

  def fromRow(row: SqlRow): OrderEntity =
    OrderEntity(
      id = row.long("id"),
      publicId = UuidBytes.fromBytes(row.byteArray("public_id")),
      userPublicId = UuidBytes.fromBytes(row.byteArray("user_public_id")),
      status = row.string("status"),
      total = row.bigDecimal("total"),
      trackingNumber = row.stringOption("tracking_number"),
      estimatedDelivery = row.localDateOption("estimated_delivery"),
      createdAt = row.instant("created_at"),
      updatedAt = row.instant("updated_at")
    )

  def toDomain(entity: OrderEntity, items: List[OrderItem]): Order =
    Order(
      id = entity.id,
      publicId = entity.publicId,
      userPublicId = entity.userPublicId,
      status = textToStatus(entity.status, entity.id),
      total = entity.total,
      trackingNumber = entity.trackingNumber.map(text => toTrackingNumber(text, entity.id)),
      estimatedDelivery = entity.estimatedDelivery,
      items = items,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt
    )

  private[persistence] def statusToText(status: OrderStatus): String = status match {
    case Pending   => "PENDING"
    case Confirmed => "CONFIRMED"
    case Shipped   => "SHIPPED"
    case Delivered => "DELIVERED"
    case Cancelled => "CANCELLED"
  }

  private def textToStatus(text: String, id: Long): OrderStatus = text match {
    case "PENDING"   => Pending
    case "CONFIRMED" => Confirmed
    case "SHIPPED"   => Shipped
    case "DELIVERED" => Delivered
    case "CANCELLED" => Cancelled
    case other       => throw new IllegalStateException(s"Unknown status '$other' for order $id")
  }

  private def toTrackingNumber(text: String, id: Long): TrackingNumber =
    TrackingNumber.from(text).fold(
      err => throw new IllegalStateException(s"Corrupt ${err.field} for order $id: ${err.message}"),
      identity
    )
}
