package com.mendev.apistore.ordering

import com.mendev.apistore.catalog.application.{GetProductSummariesUseCase, TakeStockUseCase}
import com.mendev.apistore.ordering.application.{CartSource, CheckoutUseCase, GetMyOrderUseCase, ListMyOrdersUseCase, OrderIdGenerator, OrderRepository, OrderStatusHistoryRepository, StockReservation}
import com.mendev.apistore.ordering.infrastructure.cart.InProcessCartSource
import com.mendev.apistore.ordering.infrastructure.catalog.InProcessStockReservation
import com.mendev.apistore.ordering.infrastructure.persistence.{RelateOrderRepository, RelateOrderStatusHistoryRepository}
import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shopping.application.{ClearCartUseCase, LockCartItemsUseCase}
import play.api.ContextBasedBuiltInComponents

trait OrderingComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>

  def getProductSummariesUseCase: GetProductSummariesUseCase
  def takeStockUseCase: TakeStockUseCase
  def lockCartItemsUseCase: LockCartItemsUseCase
  def clearCartUseCase: ClearCartUseCase
  def orderIdGenerator: OrderIdGenerator

  lazy val orderRepository: OrderRepository = new RelateOrderRepository
  lazy val orderStatusHistoryRepository: OrderStatusHistoryRepository = new RelateOrderStatusHistoryRepository

  lazy val cartSource: CartSource = new InProcessCartSource(lockCartItemsUseCase, clearCartUseCase)
  lazy val stockReservation: StockReservation = new InProcessStockReservation(getProductSummariesUseCase, takeStockUseCase)

  lazy val checkoutUseCase: CheckoutUseCase =
    new CheckoutUseCase(cartSource, stockReservation, orderRepository, orderStatusHistoryRepository, eventPublisher, orderIdGenerator, txRunner, clock)

  lazy val listMyOrdersUseCase: ListMyOrdersUseCase = new ListMyOrdersUseCase(orderRepository, txRunner)
  lazy val getMyOrderUseCase: GetMyOrderUseCase     = new GetMyOrderUseCase(orderRepository, txRunner)
}
