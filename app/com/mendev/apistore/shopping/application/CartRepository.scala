package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shopping.domain.CartItem

import java.sql.Connection
import java.time.Instant
import java.util.UUID

trait CartRepository {
  def getOrCreateCartId(userPublicId: UUID, now: Instant, conn: Connection): Either[AppError, Long]

  def lockCartId(userPublicId: UUID, now: Instant, conn: Connection): Either[AppError, Option[Long]]

  def findItems(userPublicId: UUID, conn: Connection): Either[AppError, List[CartItem]]

  def addQuantity(cartId: Long, productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit]

  def setQuantity(cartId: Long, productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit]

  def removeItem(cartId: Long, productId: Long, conn: Connection): Either[AppError, Unit]

  def deleteCartIfEmpty(cartId: Long, conn: Connection): Either[AppError, Unit]
}
