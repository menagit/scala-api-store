package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

trait UserRepository {
  def save(user: User, conn: Connection): Either[AppError, Unit]
  def findByEmail(email: String, conn: Connection): Either[AppError, Option[User]]
  def findByPublicId(publicId: String, conn: Connection): Either[AppError, Option[User]]

  // Atomic +1 in SQL. Old access tokens carry the old version, so they stop matching.
  def incrementTokenVersion(publicId: String, now: Instant, conn: Connection): Either[AppError, Unit]
}