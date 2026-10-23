package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.{NewProduct, Product, ProductStatus}
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

trait ProductRepository {
  // Returns the generated id.
  def insert(newProduct: NewProduct, conn: Connection): Either[AppError, Long]

  // Returns the row even when it is disabled or soft-deleted. Callers decide what to show.
  def findById(id: Long, conn: Connection): Either[AppError, Option[Product]]

  def findByIds(ids: Seq[Long], conn: Connection): Either[AppError, List[Product]]

  def findVisiblePage(categoryId: Option[Long], sort: ProductSort, order: SortOrder, limit: Int, offset: Long, conn: Connection): Either[AppError, List[Product]]

  def countVisible(categoryId: Option[Long], conn: Connection): Either[AppError, Long]

  // No status condition; deleted rows only when filter.includeDeleted.
  def findManagerPage(filter: ManagerProductFilter, sort: ProductSort, order: SortOrder, limit: Int, offset: Long, conn: Connection): Either[AppError, List[Product]]

  def countManager(filter: ManagerProductFilter, conn: Connection): Either[AppError, Long]

  def lockById(id: Long, conn: Connection): Either[AppError, Option[Product]]

  def updateStatus(id: Long, status: ProductStatus, now: Instant, conn: Connection): Either[AppError, Unit]

  def markDeleted(id: Long, now: Instant, conn: Connection): Either[AppError, Unit]
}
