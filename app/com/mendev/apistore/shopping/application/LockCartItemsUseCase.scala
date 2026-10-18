package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shopping.domain.CartItem

import java.sql.Connection
import java.time.Instant
import java.util.UUID

class LockCartItemsUseCase(cartRepo: CartRepository) {

  def executeIn(userPublicId: UUID, now: Instant, conn: Connection): Either[AppError, List[CartItem]] =
    cartRepo.lockCartId(userPublicId, now, conn).flatMap {
      case None    => Right(Nil)
      case Some(_) => cartRepo.findItems(userPublicId, conn)
    }
}
