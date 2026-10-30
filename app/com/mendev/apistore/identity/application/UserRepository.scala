package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

trait UserRepository {
  def save(user: User, conn: Connection): Either[AppError, Unit]
  def findByEmail(email: String, conn: Connection): Either[AppError, Option[User]]
  def findByPublicId(publicId: String, conn: Connection): Either[AppError, Option[User]]
  def findByPublicIds(publicIds: List[String], conn: Connection): Either[AppError, List[User]]

  // Atomic +1 in SQL. Old access tokens carry the old version, so they stop matching.
  def incrementTokenVersion(publicId: String, now: Instant, conn: Connection): Either[AppError, Unit]

  //Update hash when changing pwd
  def updatePasswordHash(publicId: String, newHash: String, now: Instant, conn: Connection): Either[AppError, Unit]

  def updateLastName(publicId: String, lastName: String, conn: Connection, now: Instant): Either[AppError, Unit]
}