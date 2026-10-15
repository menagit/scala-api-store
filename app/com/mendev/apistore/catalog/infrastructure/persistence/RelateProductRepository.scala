package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.catalog.application.{ProductRepository, ProductSort, SortOrder}
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

  override def findVisiblePage(sort: ProductSort, order: SortOrder, limit: Int, offset: Long, conn: Connection): Either[AppError, List[Product]] = {
    val entities = (sql"""
      SELECT id, name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at
      FROM catalog_product
      WHERE status = 'ACTIVE' AND deleted_at IS NULL
      ORDER BY """ + orderBy(sort, order).unsafeSql + sql"""
      LIMIT $limit OFFSET $offset
    """).asList(ProductEntity.fromRow)(conn)

    Right(entities.map(ProductEntity.toDomain))
  }

  override def countVisible(conn: Connection): Either[AppError, Long] = {
    val total = sql"""
      SELECT COUNT(*) AS total
      FROM catalog_product
      WHERE status = 'ACTIVE' AND deleted_at IS NULL
    """.asSingle(_.long("total"))(conn)

    Right(total)
  }

  private def orderBy(sort: ProductSort, order: SortOrder): String = {
    val direction = order match {
      case SortOrder.Asc  => "ASC"
      case SortOrder.Desc => "DESC"
    }
    val column = sort match {
      case ProductSort.Price     => "ROUND(price * (100 - discount_percent) / 100, 2)"
      case ProductSort.Name      => "name"
      case ProductSort.CreatedAt => "created_at"
    }
    s"$column $direction, id $direction"
  }
}
