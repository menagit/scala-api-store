package com.mendev.apistore.identity.infrastructure.persistence

import com.lucidchart.relate.SqlRow
import com.mendev.apistore.identity.application.{StoredToken, TokenType}

import java.time.Instant
import java.util.UUID

final case class TokenEntity(
                              userPublicId: UUID,
                              tokenType: String,
                              tokenHash: Array[Byte],
                              expiresAt: Instant,
                              revokedAt: Option[Instant],
                              createdAt: Instant,
                              updatedAt: Instant
                            ) {
  // The generated toString would print the hash bytes. Show only the owner and the type.
  override def toString: String = s"TokenEntity($userPublicId, $tokenType)"
}

object TokenEntity {

  def fromDomain(token: StoredToken): TokenEntity =
    TokenEntity(
      userPublicId = UUID.fromString(token.userPublicId),
      tokenType = typeToText(token.tokenType),
      tokenHash = token.tokenHash,
      expiresAt = token.expiresAt,
      revokedAt = token.revokedAt,
      createdAt = token.createdAt,
      updatedAt = token.createdAt // a new row: never updated yet
    )

  def toDomain(entity: TokenEntity): StoredToken =
    StoredToken(
      userPublicId = entity.userPublicId.toString,
      tokenType = textToType(entity.tokenType),
      tokenHash = entity.tokenHash,
      expiresAt = entity.expiresAt,
      revokedAt = entity.revokedAt,
      createdAt = entity.createdAt
    )

  // Row comes from a join with identity_user, so public_id is the owner's public id.
  def fromRow(row: SqlRow): TokenEntity =
    TokenEntity(
      userPublicId = UserEntity.bytesToUuid(row.byteArray("public_id")),
      tokenType = row.string("type"),
      tokenHash = row.byteArray("token_hash"),
      expiresAt = row.instant("expires_at"),
      revokedAt = row.instantOption("revoked_at"),
      createdAt = row.instant("created_at"),
      updatedAt = row.instant("updated_at")
    )

  private[persistence] def typeToText(tokenType: TokenType): String = tokenType match {
    case TokenType.Refresh       => "REFRESH"
    case TokenType.PasswordReset => "PASSWORD_RESET"
  }

  private def textToType(text: String): TokenType = text match {
    case "REFRESH"        => TokenType.Refresh
    case "PASSWORD_RESET" => TokenType.PasswordReset
    case other            => throw new IllegalStateException(s"Unknown token type '$other'")
  }
}