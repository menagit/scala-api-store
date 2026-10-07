package com.mendev.apistore.shared.health

import com.lucidchart.relate.*
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import play.api.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class HealthCheck (tx: TxRunner) extends Logging {

  def check(): Future[Either[AppError, Unit]] =
    tx.run[AppError, Unit] { conn =>
      sql"SELECT 1 AS ok".asSingle(_.int("ok"))(conn)
      Right(())
    }.recover { case NonFatal(e) =>
      logger.warn("Database health check failed", e)
      Left(AppError.Unavailable("Database unavailable"))
    }(ExecutionContext.parasitic)
}