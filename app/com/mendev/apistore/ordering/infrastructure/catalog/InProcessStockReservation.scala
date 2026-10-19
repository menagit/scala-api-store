package com.mendev.apistore.ordering.infrastructure.catalog

import com.mendev.apistore.catalog.application.{GetProductSummariesUseCase, ProductSummary, ReturnStockUseCase, TakeStockUseCase}
import com.mendev.apistore.ordering.application.{CheckoutProduct, StockReservation}
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

class InProcessStockReservation(
  getProductSummaries: GetProductSummariesUseCase,
  takeStock: TakeStockUseCase,
  returnStock: ReturnStockUseCase
) extends StockReservation {

  override def findProducts(ids: Seq[Long], conn: Connection): Either[AppError, List[CheckoutProduct]] =
    getProductSummaries.executeIn(ids, conn).map(_.map(toCheckoutProduct))

  override def take(productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit] =
    takeStock.executeIn(productId, quantity, now, conn)

  override def giveBack(productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit] =
    returnStock.executeIn(productId, quantity, now, conn)

  private def toCheckoutProduct(s: ProductSummary): CheckoutProduct =
    CheckoutProduct(s.id, s.name, s.available, s.stock, s.price, s.discountPercent, s.discountedPrice)
}
