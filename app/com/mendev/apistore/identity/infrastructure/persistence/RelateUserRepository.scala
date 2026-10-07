package com.mendev.apistore.identity.infrastructure.persistence

import com.mendev.apistore.identity.application.UserRepository
import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.error.AppError
import java.sql.Connection
import com.lucidchart.relate.*
import java.sql.SQLIntegrityConstraintViolationException

class RelateUserRepository extends  UserRepository{

  override def save(user: User, conn: Connection): Either[AppError, Unit] = {
    val userEntity=UserEntity.fromDomain(user)
    try {
        val SQL = sql"INSERT INTO identity_user " +
          sql" (public_id, email, password_hash, first_name, last_name, role, token_version, created_at, updated_at) "+
          sql" VALUES "+
          sql"(${UserEntity.uuidToBytes(userEntity.publicId)}, ${userEntity.email}, ${userEntity.passwordHash}, ${userEntity.firstName},"+
          sql"${userEntity.lastName}, ${userEntity.role}, ${userEntity.tokenVersion}, ${userEntity.createdAt}, ${userEntity.updatedAt}) "
      SQL.executeUpdate()(conn)
      Right(())
    }catch{
      case e: SQLIntegrityConstraintViolationException if isEmailKey(e) =>
        Left(AppError.Conflict("Email is already registered"))
    }

  }

  override def findByEmail(email: String, conn: Connection): Either[AppError, Option[User]] = {
    val entity = sql"""
      SELECT public_id, email, password_hash, first_name, last_name, role, token_version, created_at, updated_at
      FROM identity_user
      WHERE email = $email
    """.asSingleOption(UserEntity.fromRow)(conn)

    Right(entity.map(UserEntity.toDomain))
  }

  private def isEmailKey(e: SQLIntegrityConstraintViolationException): Boolean =
    Option(e.getMessage).exists(_.contains("uk_identity_user_email"))

}
