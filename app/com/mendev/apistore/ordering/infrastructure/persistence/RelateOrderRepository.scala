package com.mendev.apistore.ordering.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.ordering.application.{NewOrder, OrderRepository}
import com.mendev.apistore.ordering.domain.{Order, OrderItem, OrderStatus}
import com.mendev.apistore.shared.db.UuidBytes
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.util.UUID

class RelateOrderRepository extends OrderRepository {

  override def insert(order: NewOrder, conn: Connection): Either[AppError, Long] = {
    val id = sql"""
      INSERT INTO ordering_order
        (public_id, user_public_id, status, total, tracking_number, estimated_delivery, created_at, updated_at)
      VALUES
        (${UuidBytes.toBytes(order.publicId)}, ${UuidBytes.toBytes(order.userPublicId)},
         ${OrderEntity.statusToText(OrderStatus.Pending)}, ${order.total}, NULL, NULL,
         ${order.createdAt}, ${order.createdAt})
    """.executeInsertLong()(conn)

    order.items.foreach { item =>
      sql"""
        INSERT INTO ordering_order_item
          (order_id, product_id, product_name, unit_price, discount_percent, discounted_unit_price, quantity, line_total, created_at)
        VALUES
          ($id, ${item.productId}, ${item.productName}, ${item.unitPrice}, ${item.discountPercent},
           ${item.discountedUnitPrice}, ${item.quantity}, ${item.lineTotal}, ${order.createdAt})
      """.executeUpdate()(conn)
    }
    Right(id)
  }

  override def findByPublicId(publicId: UUID, conn: Connection): Either[AppError, Option[Order]] = {
    val entity = sql"""
      SELECT id, public_id, user_public_id, status, total, tracking_number, estimated_delivery, created_at, updated_at
      FROM ordering_order
      WHERE public_id = ${UuidBytes.toBytes(publicId)}
    """.asSingleOption(OrderEntity.fromRow)(conn)

    Right(entity.map(e => OrderEntity.toDomain(e, findItems(e.id, conn))))
  }

  override def updateStatus(order: Order, conn: Connection): Either[AppError, Unit] = {
    val updated = sql"""
      UPDATE ordering_order
      SET status = ${OrderEntity.statusToText(order.status)},
          tracking_number = ${order.trackingNumber.map(_.value)},
          estimated_delivery = ${order.estimatedDelivery},
          updated_at = ${order.updatedAt}
      WHERE id = ${order.id}
    """.executeUpdate()(conn)

    updated match {
      case 0 => throw new IllegalStateException(s"Order ${order.id} not found for status update")
      case _ => Right(())
    }
  }

  private def findItems(orderId: Long, conn: Connection): List[OrderItem] =
    sql"""
      SELECT product_id, product_name, unit_price, discount_percent, discounted_unit_price, quantity, line_total
      FROM ordering_order_item
      WHERE order_id = $orderId
      ORDER BY id
    """.asList(row =>
      OrderItem(
        productId = row.long("product_id"),
        productName = row.string("product_name"),
        unitPrice = row.bigDecimal("unit_price"),
        discountPercent = row.bigDecimal("discount_percent"),
        discountedUnitPrice = row.bigDecimal("discounted_unit_price"),
        quantity = row.int("quantity"),
        lineTotal = row.bigDecimal("line_total")
      )
    )(conn)
}
