package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.util.UUID

class ClearCartUseCase(cartRepo: CartRepository) {

  def executeIn(userPublicId: UUID, conn: Connection): Either[AppError, Unit] =
    cartRepo.deleteCart(userPublicId, conn)
}
