package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.catalog.application.{CategoryListItem, CategoryRepository}
import com.mendev.apistore.catalog.domain.Category
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class RelateCategoryRepository extends CategoryRepository {


  override def findAll(conn: Connection): Either[AppError, List[Category]] = {
    val categories = sql"""
      SELECT id, name
      FROM catalog_category
      ORDER BY id
    """.asList(row => Category(row.long("id"), row.string("name")))(conn)

    Right(categories)
  }

  override def findAllWithProductCount(conn: Connection): Either[AppError, List[CategoryListItem]] = {
    val items = sql"""
      SELECT c.id, c.name,
             (SELECT COUNT(*) FROM catalog_product p
               WHERE p.category_id = c.id AND p.status = 'ACTIVE' AND p.deleted_at IS NULL) AS product_count
      FROM catalog_category c
      ORDER BY c.name
    """.asList(row => CategoryListItem(Category(row.long("id"), row.string("name")), row.long("product_count")))(conn)

    Right(items)
  }

  override def findById(id: Long, conn: Connection): Either[AppError, Option[Category]] = {
    val category = sql"""
      SELECT id, name FROM catalog_category WHERE id = $id
    """.asSingleOption(row => Category(row.long("id"), row.string("name")))(conn)

    Right(category)
  }

  override def existsById(id: Long, conn: Connection): Either[AppError, Boolean] =
    findById(id, conn).map(_.isDefined)
}
