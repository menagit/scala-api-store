package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.{NewProduct, Product}
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

trait ProductRepository {
  // Returns the generated id.
  def insert(newProduct: NewProduct, conn: Connection): Either[AppError, Long]

  // Returns the row even when it is disabled or soft-deleted. Callers decide what to show.
  def findById(id: Long, conn: Connection): Either[AppError, Option[Product]]

  def findByIds(ids: Seq[Long], conn: Connection): Either[AppError, List[Product]]

  def findVisiblePage(sort: ProductSort, order: SortOrder, limit: Int, offset: Long, conn: Connection): Either[AppError, List[Product]]

  def countVisible(conn: Connection): Either[AppError, Long]
}
