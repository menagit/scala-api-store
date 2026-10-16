package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Actor
import com.mendev.apistore.shopping.domain.CartItem

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode.HALF_UP

class GetCartUseCase(productCatalog: ProductCatalog, cartRepo: CartRepository, runner: TxRunner)(implicit ec: ExecutionContext) {

  def execute(actor: Actor): Future[Either[AppError, CartView]] =
    runner.run(conn => cartRepo.findItems(UUID.fromString(actor.publicId), conn)).flatMap {
      case Left(e)      => Future.successful(Left(e))
      case Right(Nil)   => Future.successful(Right(CartView(Nil, BigDecimal("0.00"))))
      case Right(items) => productCatalog.findByIds(items.map(_.productId)).map(_.map(products => view(items, products)))
    }

  private def view(items: List[CartItem], products: List[CartProduct]): CartView = {
    val byId = products.map(p => p.id -> p).toMap
    val lines = items.map { item =>
      byId.get(item.productId) match {
        case Some(p) =>
          val lineTotal = (p.discountedPrice * item.quantity).setScale(2, HALF_UP)
          CartLine(p.id, p.name, item.quantity, p.price, p.discountPercent, p.discountedPrice, lineTotal, p.available)
        case None =>
          throw new IllegalStateException(s"Missing product ${item.productId} in cart")
      }
    }
    CartView(lines, lines.filter(_.available).map(_.lineTotal).sum.setScale(2, HALF_UP))
  }
}
