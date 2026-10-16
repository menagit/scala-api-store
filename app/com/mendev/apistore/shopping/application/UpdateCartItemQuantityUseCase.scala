package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.Actor

import java.time.Clock
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class UpdateCartItemQuantityUseCase(
  productCatalog: ProductCatalog,
  removeFromCartUseCase: RemoveFromCartUseCase,
  cartRepo: CartRepository,
  runner: TxRunner,
  clock: Clock
)(implicit ec: ExecutionContext) {

  def execute(actor: Actor, command: UpdateCartItemQuantityCommand): Future[Either[AppError, Unit]] =
    command.quantity match {
      case q if q < 0 =>
        Future.successful(Left(AppError.Validation("Invalid request", List(FieldError("quantity", "must be 0 or more")))))
      case 0 =>
        removeFromCartUseCase.execute(actor, command.productId)
      case q =>
        productCatalog.findByIds(Seq(command.productId)).flatMap {
          case Left(e) => Future.successful(Left(e))
          case Right(products) =>
            products.headOption.filter(_.available) match {
              case None                               => Future.successful(Left(AppError.NotFound("Product not found")))
              case Some(product) if q > product.stock => Future.successful(Left(AppError.Conflict("Not enough stock")))
              case Some(product)                      => update(UUID.fromString(actor.publicId), product.id, q)
            }
        }
    }

  private def update(user: UUID, productId: Long, quantity: Int): Future[Either[AppError, Unit]] = {
    val now = clock.instant()
    runner.run { conn =>
      cartRepo.lockCartId(user, now, conn).flatMap {
        case None         => Left(AppError.NotFound("Product not in cart"))
        case Some(cartId) => cartRepo.setQuantity(cartId, productId, quantity, now, conn)
      }
    }
  }
}
