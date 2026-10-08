package com.mendev.apistore.identity.infrastructure.persistence

import com.mendev.apistore.identity.application.{StoredToken, TokenType}
import java.util.UUID
import java.time.Instant

final case class TokenEntity(
                              userPublicId: UUID,
                              tokenType: String,
                              tokenHash: Array[Byte],
                              expiresAt: Instant,
                              createdAt: Instant,
                              updatedAt: Instant
                            ) {
  // The generated toString would print the hash bytes. Showing only the owner and the type.
  override def toString: String = s"TokenEntity($userPublicId, $tokenType)"
}

object TokenEntity {

  def fromDomain(token: StoredToken): TokenEntity =
    TokenEntity(
      userPublicId = UUID.fromString(token.userPublicId),
      tokenType = typeToText(token.tokenType),
      tokenHash = token.tokenHash,
      expiresAt = token.expiresAt,
      createdAt = token.createdAt,
      updatedAt = token.createdAt // a new row: never updated yet
    )

  private def typeToText(tokenType: TokenType): String = tokenType match {
    case TokenType.Refresh       => "REFRESH"
    case TokenType.PasswordReset => "PASSWORD_RESET"
  }
}