package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.Category
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

trait CategoryRepository {
  // Ordered by id.
  def findAll(conn: Connection): Either[AppError, List[Category]]

  // Ordered by name.
  def findAllWithProductCount(conn: Connection): Either[AppError, List[CategoryListItem]]

  def existsById(id: Long, conn: Connection): Either[AppError, Boolean]

  def findById(id: Long, conn: Connection): Either[AppError, Option[Category]]
}
