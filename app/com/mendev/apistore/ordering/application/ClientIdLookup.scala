package com.mendev.apistore.ordering.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.util.UUID

trait ClientIdLookup {
  def find(email: String, conn: Connection): Either[AppError, Option[UUID]]
}
