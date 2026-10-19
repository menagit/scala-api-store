package com.mendev.apistore.ordering.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import scala.concurrent.Future

class ListOrdersUseCase(orderRepo: OrderRepository, runner: TxRunner) {

  def execute(query: ListOrdersQuery): Future[Either[AppError, OrderPage]] = {
    val page = if (query.page < 1) 1 else query.page
    val size = if (query.size < 1) 20 else math.min(query.size, 100)
    runner.run(conn => load(page, size, conn))
  }

  private def load(page: Int, size: Int, conn: Connection): Either[AppError, OrderPage] =
    orderRepo.countAll(conn).flatMap { total =>
      orderRepo
        .findPage(size, (page - 1).toLong * size, conn)
        .map(orders => OrderPage(orders, page, size, total, (total + size - 1) / size))
    }
}
