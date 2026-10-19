package com.mendev.apistore.ordering.infrastructure.cart

import com.mendev.apistore.ordering.application.{CartSource, CheckoutLine}
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shopping.application.{ClearCartUseCase, LockCartItemsUseCase}

import java.sql.Connection
import java.time.Instant
import java.util.UUID

class InProcessCartSource(lockCartItems: LockCartItemsUseCase, clearCart: ClearCartUseCase) extends CartSource {

  override def lockItems(userPublicId: UUID, now: Instant, conn: Connection): Either[AppError, List[CheckoutLine]] =
    lockCartItems.executeIn(userPublicId, now, conn).map(_.map(item => CheckoutLine(item.productId, item.quantity)))

  override def clear(userPublicId: UUID, conn: Connection): Either[AppError, Unit] =
    clearCart.executeIn(userPublicId, conn)
}
