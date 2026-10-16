package com.mendev.apistore.shopping.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.shared.db.UuidBytes
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shopping.application.CartRepository
import com.mendev.apistore.shopping.domain.CartItem

import java.sql.Connection
import java.time.Instant
import java.util.UUID

class RelateCartRepository extends CartRepository {

  override def getOrCreateCartId(userPublicId: UUID, now: Instant, conn: Connection): Either[AppError, Long] = {
    val user = UuidBytes.toBytes(userPublicId)
    sql"""
      INSERT INTO shopping_cart (user_public_id, created_at, updated_at)
      VALUES ($user, $now, $now)
      ON DUPLICATE KEY UPDATE updated_at = $now
    """.executeUpdate()(conn)
    Right(selectId(user, conn))
  }

  override def lockCartId(userPublicId: UUID, now: Instant, conn: Connection): Either[AppError, Option[Long]] = {
    val user = UuidBytes.toBytes(userPublicId)
    val updated = sql"""
      UPDATE shopping_cart SET updated_at = $now WHERE user_public_id = $user
    """.executeUpdate()(conn)

    updated match {
      case 0 => Right(None)
      case _ => Right(Some(selectId(user, conn)))
    }
  }

  override def findItems(userPublicId: UUID, conn: Connection): Either[AppError, List[CartItem]] = {
    val items = sql"""
      SELECT i.product_id, i.quantity
      FROM shopping_cart_item i
      JOIN shopping_cart c ON c.id = i.cart_id
      WHERE c.user_public_id = ${UuidBytes.toBytes(userPublicId)}
      ORDER BY i.id
    """.asList(row => CartItem(row.long("product_id"), row.int("quantity")))(conn)

    Right(items)
  }

  override def addQuantity(cartId: Long, productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit] = {
    sql"""
      INSERT INTO shopping_cart_item (cart_id, product_id, quantity, created_at, updated_at)
      VALUES ($cartId, $productId, $quantity, $now, $now)
      ON DUPLICATE KEY UPDATE quantity = quantity + $quantity, updated_at = $now
    """.executeUpdate()(conn)
    Right(())
  }

  override def setQuantity(cartId: Long, productId: Long, quantity: Int, now: Instant, conn: Connection): Either[AppError, Unit] = {
    val updated = sql"""
      UPDATE shopping_cart_item
      SET quantity = $quantity, updated_at = $now
      WHERE cart_id = $cartId AND product_id = $productId
    """.executeUpdate()(conn)

    updated match {
      case 0 => Left(AppError.NotFound("Product not in cart"))
      case _ => Right(())
    }
  }

  override def removeItem(cartId: Long, productId: Long, conn: Connection): Either[AppError, Unit] = {
    val deleted = sql"""
      DELETE FROM shopping_cart_item WHERE cart_id = $cartId AND product_id = $productId
    """.executeUpdate()(conn)

    deleted match {
      case 0 => Left(AppError.NotFound("Product not in cart"))
      case _ => Right(())
    }
  }

  override def deleteCartIfEmpty(cartId: Long, conn: Connection): Either[AppError, Unit] = {
    sql"""
      DELETE FROM shopping_cart
      WHERE id = $cartId AND NOT EXISTS (SELECT 1 FROM shopping_cart_item WHERE cart_id = $cartId)
    """.executeUpdate()(conn)
    Right(())
  }

  private def selectId(user: Array[Byte], conn: Connection): Long =
    sql"SELECT id FROM shopping_cart WHERE user_public_id = $user".asSingle(_.long("id"))(conn)
}
