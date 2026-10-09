package com.mendev.apistore.identity.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.identity.application.{StoredToken, TokenRepository, TokenType}

import java.sql.Connection
import java.time.Instant
import java.util.UUID

class RelateTokenRepository extends TokenRepository {

  override def insert(token: StoredToken, conn: Connection): Unit = {
    val entity = TokenEntity.fromDomain(token)
    val inserted = sql"""
      INSERT INTO identity_token (user_id, type, token_hash, expires_at, created_at, updated_at)
      SELECT id, ${entity.tokenType}, ${entity.tokenHash}, ${entity.expiresAt},
             ${entity.createdAt}, ${entity.updatedAt}
      FROM identity_user
      WHERE public_id = ${UserEntity.uuidToBytes(entity.userPublicId)}
    """.executeUpdate()(conn)

    // The user must exist. Zero rows its a bug
    if (inserted != 1)
      throw new IllegalStateException(s"No user ${entity.userPublicId}: token not stored")
  }

  override def findByHash(hash: Array[Byte], conn: Connection): Option[StoredToken] =
    sql"""
      SELECT u.public_id, t.type, t.token_hash, t.expires_at, t.revoked_at, t.created_at, t.updated_at
      FROM identity_token t
      JOIN identity_user u ON u.id = t.user_id
      WHERE t.token_hash = $hash
    """.asSingleOption(TokenEntity.fromRow)(conn).map(TokenEntity.toDomain)

  // Atomic: only one caller can flip revoked_at from NULL. Exactly one row changed is the request that won
  override def revoke(hash: Array[Byte], at: Instant, conn: Connection): Boolean =
    sql"""
      UPDATE identity_token SET revoked_at = $at, updated_at = $at
      WHERE token_hash = $hash AND revoked_at IS NULL
    """.executeUpdate()(conn) == 1

  override def revokeAllForUser(userPublicId: String, at: Instant, conn: Connection): Unit = {
    val refresh = TokenEntity.typeToText(TokenType.Refresh)
    val owner   = UserEntity.uuidToBytes(UUID.fromString(userPublicId))
    sql"""
      UPDATE identity_token SET revoked_at = $at, updated_at = $at
      WHERE type = $refresh AND revoked_at IS NULL
        AND user_id = (SELECT id FROM identity_user WHERE public_id = $owner)
    """.executeUpdate()(conn)
    ()
  }

  override def deleteExpired(cutoff: Instant, limit: Int, conn: Connection): Int = {
    sql"""
      DELETE FROM identity_token WHERE EXPIRES_AT < $cutoff    LIMIT $limit
    """.executeUpdate()(conn)
  }
}