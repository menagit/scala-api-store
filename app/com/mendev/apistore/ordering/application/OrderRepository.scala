package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.Order
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.util.UUID

trait OrderRepository {
  def insert(order: NewOrder, conn: Connection): Either[AppError, Long]

  def findByPublicId(publicId: UUID, conn: Connection): Either[AppError, Option[Order]]

  def updateStatus(order: Order, conn: Connection): Either[AppError, Unit]
}
