package com.mendev.apistore.identity.application

import java.time.Instant

/** A token row in domain terms. Only the hash is stored, never the raw token. */
final case class StoredToken(
  userPublicId: String,
  tokenType: TokenType,
  tokenHash: Array[Byte],
  expiresAt: Instant,
  revokedAt: Option[Instant],
  createdAt: Instant
                            )