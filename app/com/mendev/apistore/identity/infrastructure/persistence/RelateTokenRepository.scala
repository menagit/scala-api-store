package com.mendev.apistore.identity.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.identity.application.{StoredToken, TokenRepository}

import java.sql.Connection

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

    // The user must exist. No rows it's a bug
    if (inserted != 1)
      throw new IllegalStateException(s"No user ${entity.userPublicId}: token not stored")
  }
}