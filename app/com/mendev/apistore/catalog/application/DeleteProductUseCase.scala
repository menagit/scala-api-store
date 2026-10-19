package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.{Product, ProductStatusHistoryEntry}
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Actor

import java.sql.Connection
import java.time.{Clock, Instant}
import java.util.UUID
import scala.concurrent.Future

class DeleteProductUseCase(
  productRepo: ProductRepository,
  historyRepo: ProductStatusHistoryRepository,
  pendingOrders: PendingOrderChecker,
  runner: TxRunner,
  clock: Clock
) {

  def execute(actor: Actor, productId: Long): Future[Either[AppError, Product]] = {
    val by  = UUID.fromString(actor.publicId)
    val now = clock.instant()
    runner.run(conn => delete(productId, by, now, conn))
  }

  private def delete(productId: Long, by: UUID, now: Instant, conn: Connection): Either[AppError, Product] =
    productRepo.lockById(productId, conn).flatMap {
      case Some(product) if !product.isDeleted => checkOrders(product, by, now, conn)
      case _                                   => Left(AppError.NotFound("Product not found"))
    }

  private def checkOrders(product: Product, by: UUID, now: Instant, conn: Connection): Either[AppError, Product] =
    pendingOrders.hasPendingOrders(product.id, conn).flatMap {
      case true  => Left(AppError.Conflict("Product has pending orders"))
      case false => save(product, by, now, conn)
    }

  private def save(product: Product, by: UUID, now: Instant, conn: Connection): Either[AppError, Product] =
    productRepo
      .markDeleted(product.id, now, conn)
      .flatMap(_ => historyRepo.insert(ProductStatusHistoryEntry.deleted(product.id, product.status, by, now), conn))
      .map(_ => product.copy(deletedAt = Some(now), updatedAt = now))
}
