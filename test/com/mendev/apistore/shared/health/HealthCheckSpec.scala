package com.mendev.apistore.shared.health

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.sql.{Connection, SQLException}
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class HealthCheckSpec extends AnyWordSpec with Matchers {

  private val databaseDown = new TxRunner {
    override def run[E, A](work: Connection => Either[E, A]): Future[Either[E, A]] =
      Future.failed(new SQLException("connection refused"))
  }

  "HealthCheck" should {
    "return Unavailable when the database cannot be reached" in {
      val result = Await.result(new HealthCheck(databaseDown).check(), 5.seconds)

      result shouldBe Left(AppError.Unavailable("Database unavailable"))
    }
  }
}