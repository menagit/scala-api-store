package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.catalog.application.ProductStatusHistoryRepository
import com.mendev.apistore.catalog.domain.ProductStatusHistoryEntry
import com.mendev.apistore.shared.db.UuidBytes
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class RelateProductStatusHistoryRepository extends ProductStatusHistoryRepository {

  override def insert(entry: ProductStatusHistoryEntry, conn: Connection): Either[AppError, Unit] = {
    val entity = ProductStatusHistoryEntity.fromDomain(entry)
    sql"""
      INSERT INTO catalog_product_status_history
        (product_id, action, old_status, new_status, changed_by_public_id, created_at)
      VALUES
        (${entity.productId}, ${entity.action}, ${entity.oldStatus}, ${entity.newStatus},
         ${UuidBytes.toBytes(entity.changedByPublicId)}, ${entity.createdAt})
    """.executeUpdate()(conn)
    Right(())
  }

  override def findByProductId(productId: Long, conn: Connection): Either[AppError, List[ProductStatusHistoryEntry]] = {
    val entities = sql"""
      SELECT product_id, action, old_status, new_status, changed_by_public_id, created_at
      FROM catalog_product_status_history
      WHERE product_id = $productId
      ORDER BY created_at, id
    """.asList(ProductStatusHistoryEntity.fromRow)(conn)

    Right(entities.map(ProductStatusHistoryEntity.toDomain))
  }
}
