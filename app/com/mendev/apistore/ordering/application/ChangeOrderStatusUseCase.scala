package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.OrderStatus.Cancelled
import com.mendev.apistore.ordering.domain.{ChangedBy, Order, OrderItem, OrderStatus, OrderStatusChanged, OrderStatusHistoryEntry, TrackingNumber}
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.events.EventPublisher
import com.mendev.apistore.shared.security.{Actor, Client, Manager}

import java.sql.Connection
import java.time.{Clock, Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

class ChangeOrderStatusUseCase(
  orderRepo: OrderRepository,
  historyRepo: OrderStatusHistoryRepository,
  stockReservation: StockReservation,
  eventPublisher: EventPublisher,
  runner: TxRunner,
  clock: Clock
) {

  def execute(
    actor: Actor,
    orderId: String,
    to: OrderStatus,
    trackingNumber: Option[String],
    estimatedDelivery: Option[String]
  ): Future[Either[AppError, Order]] =
    parseRequest(orderId, trackingNumber, estimatedDelivery) match {
      case Left(error) => Future.successful(Left(error))
      case Right((id, tracking, eta)) =>
        val by  = changedBy(actor)
        val now = clock.instant()
        runner.run(conn => change(id, to, by, tracking, eta, now, conn))
    }

  private def changedBy(actor: Actor): ChangedBy = actor.role match {
    case Manager => ChangedBy.Manager(UUID.fromString(actor.publicId))
    case Client  => ChangedBy.Client(UUID.fromString(actor.publicId))
  }

  private def parseRequest(
    orderId: String,
    trackingNumber: Option[String],
    estimatedDelivery: Option[String]
  ): Either[AppError, (UUID, Option[TrackingNumber], Option[LocalDate])] =
    Try(UUID.fromString(orderId)).toOption.toRight(notFound).flatMap { id =>
      val tracking = trackingNumber.map(TrackingNumber.from)
      val eta      = estimatedDelivery.map(parseDate)
      tracking.flatMap(_.left.toOption).toList ++ eta.flatMap(_.left.toOption).toList match {
        case Nil    => Right((id, tracking.flatMap(_.toOption), eta.flatMap(_.toOption)))
        case errors => Left(AppError.Validation("Invalid request", errors))
      }
    }

  private def parseDate(raw: String): Either[FieldError, LocalDate] =
    Try(LocalDate.parse(raw)).toOption.toRight(FieldError("estimatedDelivery", "must be a date (YYYY-MM-DD)"))

  private def change(
    id: UUID,
    to: OrderStatus,
    by: ChangedBy,
    tracking: Option[TrackingNumber],
    eta: Option[LocalDate],
    now: Instant,
    conn: Connection
  ): Either[AppError, Order] =
    orderRepo.lockByPublicId(id, conn).flatMap {
      case None        => Left(notFound)
      case Some(order) => order.changeStatus(to, by, tracking, eta, now).flatMap { case (changed, entry) => save(changed, entry, now, conn) }
    }

  private def save(order: Order, entry: OrderStatusHistoryEntry, now: Instant, conn: Connection): Either[AppError, Order] =
    orderRepo
      .updateStatus(order, conn)
      .flatMap(_ => historyRepo.insert(order.id, entry, conn))
      .flatMap(_ => restock(order, now, conn))
      .flatMap(_ => publish(order, entry, now, conn))

  private def publish(order: Order, entry: OrderStatusHistoryEntry, now: Instant, conn: Connection): Either[AppError, Order] = {
    eventPublisher.publish(OrderStatusChanged.of(order, entry), now, conn)
    Right(order)
  }

  private def restock(order: Order, now: Instant, conn: Connection): Either[AppError, Unit] = order.status match {
    case Cancelled => giveBackAll(order.items.sortBy(_.productId), now, conn)
    case _         => Right(())
  }

  private def giveBackAll(items: List[OrderItem], now: Instant, conn: Connection): Either[AppError, Unit] = items match {
    case Nil          => Right(())
    case item :: rest => stockReservation.giveBack(item.productId, item.quantity, now, conn).flatMap(_ => giveBackAll(rest, now, conn))
  }

  private def notFound: AppError = AppError.NotFound("Order not found")
}
