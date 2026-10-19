package com.mendev.apistore.ordering.infrastructure.catalog

import com.mendev.apistore.catalog.application.PendingOrderChecker
import com.mendev.apistore.ordering.application.OrderRepository
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class OrderingPendingOrderChecker(orderRepo: OrderRepository) extends PendingOrderChecker {

  override def hasPendingOrders(productId: Long, conn: Connection): Either[AppError, Boolean] =
    orderRepo.existsOpenOrderWithProduct(productId, conn)
}
