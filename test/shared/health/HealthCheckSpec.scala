package shared.health

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import shared.db.TxRunner
import shared.error.AppError

import java.sql.{Connection, SQLException}
import scala.concurrent.duration._
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