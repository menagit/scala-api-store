package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.{Product, ProductStatus, ProductStatusHistoryEntry}
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Actor

import java.sql.Connection
import java.time.{Clock, Instant}
import java.util.UUID
import scala.concurrent.Future

class ChangeProductStatusUseCase(
  productRepo: ProductRepository,
  historyRepo: ProductStatusHistoryRepository,
  runner: TxRunner,
  clock: Clock
) {

  def execute(actor: Actor, productId: Long, to: ProductStatus): Future[Either[AppError, Product]] = {
    val by  = UUID.fromString(actor.publicId)
    val now = clock.instant()
    runner.run(conn => change(productId, to, by, now, conn))
  }

  private def change(productId: Long, to: ProductStatus, by: UUID, now: Instant, conn: Connection): Either[AppError, Product] =
    productRepo.lockById(productId, conn).flatMap {
      case Some(product) if !product.isDeleted && product.status == to => Right(product)
      case Some(product) if !product.isDeleted                         => save(product, to, by, now, conn)
      case _                                                           => Left(AppError.NotFound("Product not found"))
    }

  private def save(product: Product, to: ProductStatus, by: UUID, now: Instant, conn: Connection): Either[AppError, Product] =
    productRepo
      .updateStatus(product.id, to, now, conn)
      .flatMap(_ => historyRepo.insert(ProductStatusHistoryEntry.statusChanged(product.id, product.status, to, by, now), conn))
      .map(_ => product.copy(status = to, updatedAt = now))
}
