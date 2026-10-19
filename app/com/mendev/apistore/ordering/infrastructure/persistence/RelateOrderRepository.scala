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

  override def lockByPublicId(publicId: UUID, conn: Connection): Either[AppError, Option[Order]] = {
    val entity = sql"""
      SELECT id, public_id, user_public_id, status, total, tracking_number, estimated_delivery, created_at, updated_at
      FROM ordering_order
      WHERE public_id = ${UuidBytes.toBytes(publicId)}
      FOR UPDATE
    """.asSingleOption(OrderEntity.fromRow)(conn)

    Right(entity.map(e => OrderEntity.toDomain(e, findItems(e.id, conn))))
  }

  override def findPageByUser(userPublicId: UUID, limit: Int, offset: Long, conn: Connection): Either[AppError, List[Order]] = {
    val entities = sql"""
      SELECT id, public_id, user_public_id, status, total, tracking_number, estimated_delivery, created_at, updated_at
      FROM ordering_order
      WHERE user_public_id = ${UuidBytes.toBytes(userPublicId)}
      ORDER BY created_at DESC, id DESC
      LIMIT $limit OFFSET $offset
    """.asList(OrderEntity.fromRow)(conn)

    Right(withItems(entities, conn))
  }

  override def countByUser(userPublicId: UUID, conn: Connection): Either[AppError, Long] = {
    val total = sql"""
      SELECT COUNT(*) AS total
      FROM ordering_order
      WHERE user_public_id = ${UuidBytes.toBytes(userPublicId)}
    """.asSingle(_.long("total"))(conn)

    Right(total)
  }

  override def findPage(limit: Int, offset: Long, conn: Connection): Either[AppError, List[Order]] = {
    val entities = sql"""
      SELECT id, public_id, user_public_id, status, total, tracking_number, estimated_delivery, created_at, updated_at
      FROM ordering_order
      ORDER BY created_at DESC, id DESC
      LIMIT $limit OFFSET $offset
    """.asList(OrderEntity.fromRow)(conn)

    Right(withItems(entities, conn))
  }

  override def countAll(conn: Connection): Either[AppError, Long] = {
    val total = sql"""
      SELECT COUNT(*) AS total
      FROM ordering_order
    """.asSingle(_.long("total"))(conn)

    Right(total)
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

  private def withItems(entities: List[OrderEntity], conn: Connection): List[Order] = entities match {
    case Nil => Nil
    case _ =>
      val byOrder = findItemsByOrders(entities.map(_.id), conn)
      entities.map(e => OrderEntity.toDomain(e, byOrder.getOrElse(e.id, Nil)))
  }

  private def findItems(orderId: Long, conn: Connection): List[OrderItem] =
    sql"""
      SELECT product_id, product_name, unit_price, discount_percent, discounted_unit_price, quantity, line_total
      FROM ordering_order_item
      WHERE order_id = $orderId
      ORDER BY id
    """.asList(itemFromRow)(conn)

  private def findItemsByOrders(orderIds: List[Long], conn: Connection): Map[Long, List[OrderItem]] =
    sql"""
      SELECT order_id, product_id, product_name, unit_price, discount_percent, discounted_unit_price, quantity, line_total
      FROM ordering_order_item
      WHERE order_id IN ($orderIds)
      ORDER BY id
    """.asList(row => row.long("order_id") -> itemFromRow(row))(conn).groupMap(_._1)(_._2)

  private def itemFromRow(row: SqlRow): OrderItem =
    OrderItem(
      productId = row.long("product_id"),
      productName = row.string("product_name"),
      unitPrice = row.bigDecimal("unit_price"),
      discountPercent = row.bigDecimal("discount_percent"),
      discountedUnitPrice = row.bigDecimal("discounted_unit_price"),
      quantity = row.int("quantity"),
      lineTotal = row.bigDecimal("line_total")
    )
}
