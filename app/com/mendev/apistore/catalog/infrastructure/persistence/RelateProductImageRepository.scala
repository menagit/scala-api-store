package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.catalog.application.ProductImageRepository
import com.mendev.apistore.catalog.domain.ProductImage
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

class RelateProductImageRepository extends ProductImageRepository {

  override def insert(productId: Long, position: Int, largeKey: String, thumbKey: String, now: Instant, conn: Connection): Either[AppError, Long] = {
    val id = sql"""
      INSERT INTO catalog_product_image (product_id, position, large_key, thumb_key, created_at)
      VALUES ($productId, $position, $largeKey, $thumbKey, $now)
    """.executeInsertLong()(conn)
    Right(id)
  }

  override def countByProduct(productId: Long, conn: Connection): Either[AppError, Int] = {
    val total = sql"""
      SELECT COUNT(*) AS total
      FROM catalog_product_image
      WHERE product_id = $productId
    """.asSingle(_.int("total"))(conn)
    Right(total)
  }

  override def nextPosition(productId: Long, conn: Connection): Either[AppError, Int] = {
    val next = sql"""
      SELECT COALESCE(MAX(position), 0) + 1 AS next_position
      FROM catalog_product_image
      WHERE product_id = $productId
    """.asSingle(_.int("next_position"))(conn)
    Right(next)
  }

  override def findByProduct(productId: Long, conn: Connection): Either[AppError, List[ProductImage]] = {
    val images = sql"""
      SELECT id, product_id, position, large_key, thumb_key, created_at
      FROM catalog_product_image
      WHERE product_id = $productId
      ORDER BY position
    """.asList(fromRow)(conn)
    Right(images)
  }

  override def findByProducts(productIds: List[Long], conn: Connection): Either[AppError, Map[Long, List[ProductImage]]] = productIds match {
    case Nil => Right(Map.empty)
    case _ =>
      val images = sql"""
        SELECT id, product_id, position, large_key, thumb_key, created_at
        FROM catalog_product_image
        WHERE product_id IN ($productIds)
        ORDER BY product_id, position
      """.asList(fromRow)(conn)
      Right(images.groupBy(_.productId))
  }

  override def findById(imageId: Long, productId: Long, conn: Connection): Either[AppError, Option[ProductImage]] = {
    val image = sql"""
      SELECT id, product_id, position, large_key, thumb_key, created_at
      FROM catalog_product_image
      WHERE id = $imageId AND product_id = $productId
    """.asSingleOption(fromRow)(conn)
    Right(image)
  }

  override def delete(imageId: Long, conn: Connection): Either[AppError, Unit] = {
    sql"""
      DELETE FROM catalog_product_image
      WHERE id = $imageId
    """.executeUpdate()(conn)
    Right(())
  }

  private def fromRow(row: SqlRow): ProductImage =
    ProductImage(
      id = row.long("id"),
      productId = row.long("product_id"),
      position = row.int("position"),
      largeKey = row.string("large_key"),
      thumbKey = row.string("thumb_key"),
      createdAt = row.instant("created_at")
    )
}
