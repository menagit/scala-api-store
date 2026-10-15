package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.catalog.application.ProductRepository
import com.mendev.apistore.catalog.domain.{NewProduct, Product}
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class RelateProductRepository extends ProductRepository {


  override def insert(newProduct: NewProduct, conn: Connection): Either[AppError, Long] = {
    val id = sql"""
      INSERT INTO catalog_product
        (name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at)
      VALUES
        (${newProduct.name}, ${newProduct.description}, ${newProduct.categoryId}, ${newProduct.price.value},
         ${newProduct.discount.percent}, ${newProduct.stock.value}, ${ProductEntity.statusToText(newProduct.status)},
         NULL, ${newProduct.createdAt}, ${newProduct.updatedAt})
    """.executeInsertLong()(conn)
    Right(id)
  }

  override def findById(id: Long, conn: Connection): Either[AppError, Option[Product]] = {
    val entity = sql"""
      SELECT id, name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at
      FROM catalog_product
      WHERE id = $id
    """.asSingleOption(ProductEntity.fromRow)(conn)

    Right(entity.map(ProductEntity.toDomain))
  }
}
