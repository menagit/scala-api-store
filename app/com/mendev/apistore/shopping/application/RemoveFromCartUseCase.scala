package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Actor

import java.time.Clock
import java.util.UUID
import scala.concurrent.Future

class RemoveFromCartUseCase(cartRepo: CartRepository, runner: TxRunner, clock: Clock) {

  def execute(actor: Actor, productId: Long): Future[Either[AppError, Unit]] = {
    val user = UUID.fromString(actor.publicId)
    val now = clock.instant()
    runner.run { conn =>
      cartRepo.lockCartId(user, now, conn).flatMap {
        case None         => Left(AppError.NotFound("Product not in cart"))
        case Some(cartId) => cartRepo.removeItem(cartId, productId, conn).flatMap(_ => cartRepo.deleteCartIfEmpty(cartId, conn))
      }
    }
  }
}
