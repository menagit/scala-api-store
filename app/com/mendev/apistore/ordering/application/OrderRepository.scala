package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.Order
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.util.UUID

trait OrderRepository {
  def insert(order: NewOrder, conn: Connection): Either[AppError, Long]

  def findByPublicId(publicId: UUID, conn: Connection): Either[AppError, Option[Order]]

  def findPageByUser(userPublicId: UUID, limit: Int, offset: Long, conn: Connection): Either[AppError, List[Order]]

  def countByUser(userPublicId: UUID, conn: Connection): Either[AppError, Long]

  def updateStatus(order: Order, conn: Connection): Either[AppError, Unit]
}
