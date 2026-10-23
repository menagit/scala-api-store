package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.catalog.application.ProductPriceHistoryRepository
import com.mendev.apistore.catalog.domain.ProductPriceChange
import com.mendev.apistore.shared.db.UuidBytes
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class RelateProductPriceHistoryRepository extends ProductPriceHistoryRepository {

  override def insert(change: ProductPriceChange, conn: Connection): Either[AppError, Unit] = {
    sql"""
      INSERT INTO catalog_product_price_history
        (product_id, old_price, new_price, old_discount_percent, new_discount_percent, changed_by_public_id, created_at)
      VALUES
        (${change.productId}, ${change.oldPrice.value}, ${change.newPrice.value},
         ${change.oldDiscount.percent}, ${change.newDiscount.percent},
         ${UuidBytes.toBytes(change.changedByPublicId)}, ${change.createdAt})
    """.executeUpdate()(conn)
    Right(())
  }
}
