package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.{ChangedBy, Order, OrderItem, OrderStatusChanged, OrderStatusHistoryEntry}
import com.mendev.apistore.ordering.domain.OrderStatus.{Confirmed, Pending}
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.events.EventPublisher
import com.mendev.apistore.shared.security.Actor

import java.sql.Connection
import java.time.{Clock, Instant}
import java.util.UUID
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode.HALF_UP

class CheckoutUseCase(
  cartSource: CartSource,
  stockReservation: StockReservation,
  orderRepo: OrderRepository,
  historyRepo: OrderStatusHistoryRepository,
  eventPublisher: EventPublisher,
  idGenerator: OrderIdGenerator,
  runner: TxRunner,
  clock: Clock
) {

  def execute(actor: Actor): Future[Either[AppError, Order]] = {
    val user = UUID.fromString(actor.publicId)
    val now  = clock.instant()
    runner.run(conn => checkout(user, now, conn))
  }

  private def checkout(user: UUID, now: Instant, conn: Connection): Either[AppError, Order] =
    cartSource.lockItems(user, now, conn).flatMap {
      case Nil   => Left(AppError.Validation("Cart is empty"))
      case lines => buy(user, lines.sortBy(_.productId), now, conn)
    }

  private def buy(user: UUID, lines: List[CheckoutLine], now: Instant, conn: Connection): Either[AppError, Order] =
    reserve(lines, now, conn)
      .flatMap(items => placeOrder(user, items, now, conn))
      .flatMap(order => cartSource.clear(user, conn).map(_ => order))

  private def reserve(lines: List[CheckoutLine], now: Instant, conn: Connection): Either[AppError, List[OrderItem]] =
    stockReservation
      .findProducts(lines.map(_.productId), conn)
      .flatMap(products => checkAll(lines, products))
      .flatMap(items => takeAll(items, now, conn).map(_ => items))

  private def checkAll(lines: List[CheckoutLine], products: List[CheckoutProduct]): Either[AppError, List[OrderItem]] = {
    val byId    = products.map(p => p.id -> p).toMap
    val checked = lines.map(line => check(line, byId.get(line.productId)))
    checked.collect { case Left(failure) => failure } match {
      case Nil      => Right(checked.collect { case Right(item) => item })
      case failures => Left(AppError.Conflict("Cannot check out: " + failures.mkString("; ")))
    }
  }

  private def check(line: CheckoutLine, product: Option[CheckoutProduct]): Either[String, OrderItem] = product match {
    case Some(p) if p.available && line.quantity <= p.stock => Right(toItem(line, p))
    case Some(p) if p.available                             => Left(s"product ${p.id} has only ${p.stock} in stock")
    case _                                                  => Left(s"product ${line.productId} is unavailable")
  }

  private def toItem(line: CheckoutLine, p: CheckoutProduct): OrderItem =
    OrderItem(p.id, p.name, p.price, p.discountPercent, p.discountedPrice, line.quantity, (p.discountedPrice * line.quantity).setScale(2, HALF_UP))

  private def takeAll(items: List[OrderItem], now: Instant, conn: Connection): Either[AppError, Unit] = items match {
    case Nil          => Right(())
    case item :: rest => stockReservation.take(item.productId, item.quantity, now, conn).flatMap(_ => takeAll(rest, now, conn))
  }

  private def placeOrder(user: UUID, items: List[OrderItem], now: Instant, conn: Connection): Either[AppError, Order] = {
    val total    = items.map(_.lineTotal).sum.setScale(2, HALF_UP)
    val newOrder = NewOrder(idGenerator.next(), user, total, items, now)
    val created  = OrderStatusHistoryEntry.created(user, now)
    orderRepo
      .insert(newOrder, conn)
      .flatMap(id => historyRepo.insert(id, created, conn).map(_ => pendingOrder(id, newOrder)))
      .flatMap(pending => confirm(pending, created, now, conn))
  }

  private def pendingOrder(id: Long, o: NewOrder): Order =
    Order(id, o.publicId, o.userPublicId, Pending, o.total, None, None, o.items, o.createdAt, o.createdAt)

  private def confirm(pending: Order, created: OrderStatusHistoryEntry, now: Instant, conn: Connection): Either[AppError, Order] =
    pending.changeStatus(Confirmed, ChangedBy.System, None, None, now).flatMap { case (confirmed, entry) =>
      orderRepo
        .updateStatus(confirmed, conn)
        .flatMap(_ => historyRepo.insert(confirmed.id, entry, conn))
        .flatMap(_ => publish(confirmed, List(created, entry), now, conn))
    }

  private def publish(order: Order, entries: List[OrderStatusHistoryEntry], now: Instant, conn: Connection): Either[AppError, Order] = {
    entries.foreach(entry => eventPublisher.publish(OrderStatusChanged.of(order, entry), now, conn))
    Right(order)
  }
}
