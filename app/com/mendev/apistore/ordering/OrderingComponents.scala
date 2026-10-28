package com.mendev.apistore.ordering

import com.mendev.apistore.catalog.application.{GetProductSummariesUseCase, PendingOrderChecker, ReturnStockUseCase, TakeStockUseCase}
import com.mendev.apistore.ordering.application.{CartSource, ChangeOrderStatusUseCase, CheckoutUseCase, ClientDirectory, ClientIdLookup, GetMyOrderUseCase, GetOrderClientsUseCase, ListMyOrdersUseCase, ListOrdersUseCase, OrderIdGenerator, OrderRepository, OrderStatusHistoryRepository, StockReservation}
import com.mendev.apistore.ordering.infrastructure.cart.InProcessCartSource
import com.mendev.apistore.ordering.infrastructure.catalog.{InProcessStockReservation, OrderingPendingOrderChecker}
import com.mendev.apistore.ordering.infrastructure.persistence.{RelateOrderRepository, RelateOrderStatusHistoryRepository}
import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shopping.application.{ClearCartUseCase, LockCartItemsUseCase}
import play.api.ContextBasedBuiltInComponents

trait OrderingComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>

  def getProductSummariesUseCase: GetProductSummariesUseCase
  def takeStockUseCase: TakeStockUseCase
  def returnStockUseCase: ReturnStockUseCase
  def lockCartItemsUseCase: LockCartItemsUseCase
  def clearCartUseCase: ClearCartUseCase
  def orderIdGenerator: OrderIdGenerator
  def clientIdLookup: ClientIdLookup
  def clientDirectory: ClientDirectory

  lazy val orderRepository: OrderRepository = new RelateOrderRepository
  lazy val orderStatusHistoryRepository: OrderStatusHistoryRepository = new RelateOrderStatusHistoryRepository

  lazy val cartSource: CartSource = new InProcessCartSource(lockCartItemsUseCase, clearCartUseCase)
  lazy val stockReservation: StockReservation =
    new InProcessStockReservation(getProductSummariesUseCase, takeStockUseCase, returnStockUseCase)
  lazy val pendingOrderChecker: PendingOrderChecker = new OrderingPendingOrderChecker(orderRepository)

  lazy val checkoutUseCase: CheckoutUseCase =
    new CheckoutUseCase(cartSource, stockReservation, orderRepository, orderStatusHistoryRepository, eventPublisher, orderIdGenerator, txRunner, clock)

  lazy val listMyOrdersUseCase: ListMyOrdersUseCase = new ListMyOrdersUseCase(orderRepository, txRunner)
  lazy val getMyOrderUseCase: GetMyOrderUseCase     = new GetMyOrderUseCase(orderRepository, txRunner)
  lazy val listOrdersUseCase: ListOrdersUseCase     = new ListOrdersUseCase(orderRepository, clientIdLookup, txRunner)
  lazy val getOrderClientsUseCase: GetOrderClientsUseCase = new GetOrderClientsUseCase(clientDirectory, txRunner)

  lazy val changeOrderStatusUseCase: ChangeOrderStatusUseCase =
    new ChangeOrderStatusUseCase(orderRepository, orderStatusHistoryRepository, stockReservation, eventPublisher, txRunner, clock)
}
