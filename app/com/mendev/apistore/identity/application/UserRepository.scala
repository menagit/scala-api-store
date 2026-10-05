package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

trait UserRepository {
  def save(user: User, conn: Connection): Either[AppError, Unit]
}