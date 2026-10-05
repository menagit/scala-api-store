package com.mendev.apistore.shared.db

import play.api.db.Database

import java.sql.Connection
import scala.concurrent.{ExecutionContext, Future}

class PlayDbTxRunner(db: Database, ec: ExecutionContext) extends TxRunner {

  override def run[E, A](work: Connection => Either[E, A]): Future[Either[E, A]] = {
    Future {
      db.withTransaction { conn =>
        val result = work(conn)
        if (result.isLeft) conn.rollback()
        result
      }
    }(ec)
  }
}