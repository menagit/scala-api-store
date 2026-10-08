package com.mendev.apistore.identity.application

import java.sql.Connection
import java.time.Instant

trait TokenRepository {
  def insert(token: StoredToken, conn: Connection): Unit

  def findByHash(hash: Array[Byte], conn: Connection): Option[StoredToken]

  // True -> this call revoked it. False -> already revoked
  def revoke(hash: Array[Byte], at: Instant, conn: Connection): Boolean

  def revokeAllForUser(userPublicId: String, at: Instant, conn: Connection): Unit
}