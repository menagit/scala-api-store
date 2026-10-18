package com.mendev.apistore.ordering.domain

import com.mendev.apistore.ordering.domain.ChangedBy.{Client, Manager, System}
import com.mendev.apistore.ordering.domain.OrderStatus.{Cancelled, Confirmed, Delivered, Pending, Shipped}
import com.mendev.apistore.shared.error.{AppError, FieldError}

import java.time.{Instant, LocalDate}
import java.util.UUID

final case class Order(
  id: Long,
  publicId: UUID,
  userPublicId: UUID,
  status: OrderStatus,
  total: BigDecimal,
  trackingNumber: Option[TrackingNumber],
  estimatedDelivery: Option[LocalDate],
  items: List[OrderItem],
  createdAt: Instant,
  updatedAt: Instant
) {

  def changeStatus(
    to: OrderStatus,
    by: ChangedBy,
    trackingNumber: Option[TrackingNumber],
    estimatedDelivery: Option[LocalDate],
    now: Instant
  ): Either[AppError, (Order, OrderStatusHistoryEntry)] =
    if (!isValidMove(to)) Left(AppError.Conflict(s"Invalid status change from ${label(status)} to ${label(to)}"))
    else if (!isAllowed(to, by)) Left(AppError.Forbidden())
    else missingShippingFields(to, trackingNumber, estimatedDelivery) match {
      case Nil    => Right((moveTo(to, trackingNumber, estimatedDelivery, now), OrderStatusHistoryEntry.changed(status, to, by, now)))
      case errors => Left(AppError.Validation("Invalid request", errors))
    }

  private def isValidMove(to: OrderStatus): Boolean = (status, to) match {
    case (Pending, Confirmed) | (Pending, Cancelled) | (Confirmed, Shipped) | (Confirmed, Cancelled) | (Shipped, Delivered) => true
    case _ => false
  }

  private def isAllowed(to: OrderStatus, by: ChangedBy): Boolean = (status, to, by) match {
    case (Pending, Confirmed, System)                 => true
    case (Pending, Cancelled, Client(_) | System)     => true
    case (Confirmed, Shipped | Cancelled, Manager(_)) => true
    case (Shipped, Delivered, Manager(_))             => true
    case _                                            => false
  }

  private def missingShippingFields(
    to: OrderStatus,
    trackingNumber: Option[TrackingNumber],
    estimatedDelivery: Option[LocalDate]
  ): List[FieldError] = to match {
    case Shipped =>
      List(
        Option.when(trackingNumber.isEmpty)(FieldError("trackingNumber", "is required")),
        Option.when(estimatedDelivery.isEmpty)(FieldError("estimatedDelivery", "is required"))
      ).flatten
    case _ => Nil
  }

  private def moveTo(
    to: OrderStatus,
    trackingNumber: Option[TrackingNumber],
    estimatedDelivery: Option[LocalDate],
    now: Instant
  ): Order = to match {
    case Shipped => copy(status = to, trackingNumber = trackingNumber, estimatedDelivery = estimatedDelivery, updatedAt = now)
    case _       => copy(status = to, updatedAt = now)
  }

  private def label(s: OrderStatus): String = s.toString.toUpperCase
}
