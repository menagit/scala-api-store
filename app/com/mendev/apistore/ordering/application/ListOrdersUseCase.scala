package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.OrderStatus
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}

import java.sql.Connection
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.Locale
import scala.concurrent.Future
import scala.util.Try

class ListOrdersUseCase(orderRepo: OrderRepository, clientIdLookup: ClientIdLookup, runner: TxRunner) {

  def execute(query: ListOrdersQuery): Future[Either[AppError, OrderPage]] = {
    val page   = if (query.page < 1) 1 else query.page
    val size   = if (query.size < 1) 20 else math.min(query.size, 100)
    val status = parseStatus(query.status)
    val from   = parseDate("createdFrom", query.createdFrom)
    val to     = parseDate("createdTo", query.createdTo)
    val range  = checkRange(from, to)
    val email  = parseEmail(query.clientEmail)
    (status, from, to, range, email) match {
      case (Right(s), Right(f), Right(t), Right(_), Right(e)) =>
        val filter = OrderFilter(s, f.map(startOf), t.map(d => startOf(d.plusDays(1))), None)
        runner.run(conn => load(filter, e, page, size, conn))
      case _ =>
        val errors = List(status, from, to, range, email).collect { case Left(e) => e }
        Future.successful(Left(AppError.Validation("Invalid request", errors)))
    }
  }

  private def load(filter: OrderFilter, email: Option[String], page: Int, size: Int, conn: Connection): Either[AppError, OrderPage] =
    email match {
      case None => loadPage(filter, page, size, conn)
      case Some(e) =>
        clientIdLookup.find(e, conn).flatMap {
          case None     => Right(OrderPage(Nil, page, size, 0, 0))
          case Some(id) => loadPage(filter.copy(userPublicId = Some(id)), page, size, conn)
        }
    }

  private def loadPage(filter: OrderFilter, page: Int, size: Int, conn: Connection): Either[AppError, OrderPage] =
    orderRepo.count(filter, conn).flatMap { total =>
      orderRepo
        .findPage(filter, size, (page - 1).toLong * size, conn)
        .map(orders => OrderPage(orders, page, size, total, (total + size - 1) / size))
    }

  private def parseStatus(text: Option[String]): Either[FieldError, Option[OrderStatus]] = text match {
    case None              => Right(None)
    case Some("PENDING")   => Right(Some(OrderStatus.Pending))
    case Some("CONFIRMED") => Right(Some(OrderStatus.Confirmed))
    case Some("SHIPPED")   => Right(Some(OrderStatus.Shipped))
    case Some("DELIVERED") => Right(Some(OrderStatus.Delivered))
    case Some("CANCELLED") => Right(Some(OrderStatus.Cancelled))
    case Some(_)           => Left(FieldError("status", "must be PENDING, CONFIRMED, SHIPPED, DELIVERED or CANCELLED"))
  }

  private def parseDate(field: String, text: Option[String]): Either[FieldError, Option[LocalDate]] = text match {
    case None    => Right(None)
    case Some(t) => Try(LocalDate.parse(t)).toOption.map(Some(_)).toRight(FieldError(field, "must be a date (YYYY-MM-DD)"))
  }

  private def checkRange(from: Either[FieldError, Option[LocalDate]], to: Either[FieldError, Option[LocalDate]]): Either[FieldError, Unit] =
    (from, to) match {
      case (Right(Some(f)), Right(Some(t))) if f.isAfter(t) => Left(FieldError("createdTo", "must not be before createdFrom"))
      case _                                                => Right(())
    }

  private def parseEmail(text: Option[String]): Either[FieldError, Option[String]] = text match {
    case None                 => Right(None)
    case Some(t) if t.isBlank => Left(FieldError("clientEmail", "must not be blank"))
    case Some(t)              => Right(Some(t.strip().toLowerCase(Locale.ROOT)))
  }

  private def startOf(date: LocalDate): Instant = date.atStartOfDay(ZoneOffset.UTC).toInstant
}
