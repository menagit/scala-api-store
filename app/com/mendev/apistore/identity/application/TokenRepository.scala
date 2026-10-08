package com.mendev.apistore.identity.application

import java.sql.Connection

trait TokenRepository {
  def insert(token: StoredToken, conn: Connection): Unit
}