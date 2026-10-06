package com.mendev.apistore.identity.infrastructure.persistence

import com.mendev.apistore.identity.application.UserRepository
import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.error.AppError
import jakarta.inject.Singleton
import java.nio.ByteBuffer
import java.util.UUID
import java.sql.Connection
import com.lucidchart.relate.*
import java.sql.SQLIntegrityConstraintViolationException

@Singleton
class RelateUserRepository extends  UserRepository{

  override def save(user: User, conn: Connection): Either[AppError, Unit] = {
    val userEntity=UserEntity.fromDomain(user)
    try {
        val SQL = sql"INSERT INTO identity_user " +
          sql" (public_id, email, password_hash, first_name, last_name, role, token_version, created_at, updated_at) "+
          sql" VALUES "+
          sql"(${uuidToBytes(userEntity.publicId)}, ${userEntity.email}, ${userEntity.passwordHash}, ${userEntity.firstName},"+
          sql"${userEntity.lastName}, ${userEntity.role}, ${userEntity.tokenVersion}, ${userEntity.createdAt}, ${userEntity.updatedAt}) "
      SQL.executeUpdate()(conn)
      Right(())
    }catch{
      case e: SQLIntegrityConstraintViolationException if isEmailKey(e) =>
        Left(AppError.Conflict("Email is already registered"))
    }

  }

  private def isEmailKey(e: SQLIntegrityConstraintViolationException): Boolean =
    Option(e.getMessage).exists(_.contains("uk_identity_user_email"))

  /** Conversion Helper Method
   * @param uuid
   * @return
   */
  private def uuidToBytes(uuid: java.util.UUID): Array[Byte]={
    ByteBuffer
      //empty 16 byte buffer
      .allocate(16)
      //writes 8 bytes and return the same buffer
      .putLong(uuid.getMostSignificantBits)
      //writes 8 bytes and return the same buffer
      .putLong(uuid.getLeastSignificantBits)
      .array()
  }
}
