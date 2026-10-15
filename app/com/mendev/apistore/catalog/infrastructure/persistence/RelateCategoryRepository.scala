package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.catalog.application.CategoryRepository
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
}
