package com.mendev.apistore.identity.infrastructure.persistence

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.security.{Client, Manager, Role}
import java.time.Instant
import java.util.UUID

final case class UserEntity(
  publicId: UUID,
  email: String,
  passwordHash: String,
  firstName: String,
  lastName: String,
  role: String,
  tokenVersion: Int,
  createdAt: Instant,
  updatedAt: Instant
                           ) {
  // The generated toString would print the password hash. Show only the public id.
  override def toString: String = s"UserEntity($publicId)"
}

//Mappers in the companion object

object UserEntity {


  def fromDomain(user: User): UserEntity =
    UserEntity(
      publicId = UUID.fromString(user.publicId),
      email = user.email,
      passwordHash = user.passwordHash,
      firstName = user.firstName,
      lastName = user.lastName,
      role = roleToText(user.role),
      tokenVersion = user.tokenVersion,
      createdAt = user.createdAt,
      updatedAt = user.updatedAt
    )

  def toDomain(entity: UserEntity): User =
    User(
      publicId = entity.publicId.toString,
      email = entity.email,
      firstName = entity.firstName,
      lastName = entity.lastName,
      passwordHash = entity.passwordHash,
      role = textToRole(entity.role, entity.publicId),
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt,
      tokenVersion = entity.tokenVersion
    )

  private def roleToText(role: Role): String = role match {
    case Manager => "MANAGER"
    case Client  => "CLIENT"
  }

  private def textToRole(text: String, publicId: UUID): Role = text match {
    case "MANAGER" => Manager
    case "CLIENT"  => Client
    case other     => throw new IllegalStateException(s"Unknown role '$other' for user $publicId")
  }
}