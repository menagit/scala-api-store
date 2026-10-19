package com.mendev.apistore.ordering.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Actor

import java.sql.Connection
import java.util.UUID
import scala.concurrent.Future

class ListMyOrdersUseCase(orderRepo: OrderRepository, runner: TxRunner) {

  def execute(actor: Actor, query: ListMyOrdersQuery): Future[Either[AppError, OrderPage]] = {
    val user = UUID.fromString(actor.publicId)
    val page = if (query.page < 1) 1 else query.page
    val size = if (query.size < 1) 20 else math.min(query.size, 100)
    runner.run(conn => load(user, page, size, conn))
  }

  private def load(user: UUID, page: Int, size: Int, conn: Connection): Either[AppError, OrderPage] =
    orderRepo.countByUser(user, conn).flatMap { total =>
      orderRepo
        .findPageByUser(user, size, (page - 1).toLong * size, conn)
        .map(orders => OrderPage(orders, page, size, total, (total + size - 1) / size))
    }
}
