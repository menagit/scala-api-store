package com.mendev.apistore.ordering.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.util.UUID

trait ClientDirectory {
  def find(userPublicIds: List[UUID], conn: Connection): Either[AppError, List[ClientInfo]]
}
