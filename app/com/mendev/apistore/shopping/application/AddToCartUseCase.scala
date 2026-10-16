package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.Actor

import java.time.Clock
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AddToCartUseCase(
  productCatalog: ProductCatalog,
  cartRepo: CartRepository,
  runner: TxRunner,
  clock: Clock
)(implicit ec: ExecutionContext) {

  def execute(actor: Actor, command: AddToCartCommand): Future[Either[AppError, Unit]] =
    if (command.quantity <= 0)
      Future.successful(Left(AppError.Validation("Invalid request", List(FieldError("quantity", "must be greater than 0")))))
    else
      productCatalog.findByIds(Seq(command.productId)).flatMap {
        case Left(e) => Future.successful(Left(e))
        case Right(products) =>
          products.headOption.filter(_.available) match {
            case Some(product) => add(UUID.fromString(actor.publicId), product, command.quantity)
            case None          => Future.successful(Left(AppError.NotFound("Product not found")))
          }
      }

  private def add(user: UUID, product: CartProduct, quantity: Int): Future[Either[AppError, Unit]] = {
    val now = clock.instant()
    runner.run { conn =>
      cartRepo.findItems(user, conn).flatMap { items =>
        val inCart = items.find(_.productId == product.id).map(_.quantity).getOrElse(0)
        if (inCart + quantity > product.stock) Left(AppError.Conflict("Not enough stock"))
        else cartRepo.getOrCreateCartId(user, now, conn).flatMap(cartId => cartRepo.addQuantity(cartId, product.id, quantity, now, conn))
      }
    }
  }
}
