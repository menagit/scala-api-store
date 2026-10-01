package shared.db

import java.sql.Connection
import java.util.concurrent.Executors

import com.lucidchart.relate._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.db.Databases

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class TxRunnerSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  private val pool = Executors.newFixedThreadPool(2)
  private val ec = ExecutionContext.fromExecutorService(pool)
  private val cfg = TestDatabase.config
  private val db = Databases(
    "com.mysql.cj.jdbc.Driver",
    s"jdbc:mysql://${cfg.host}:${cfg.port}/${cfg.name}",
    "test",
    Map("username" -> cfg.user, "password" -> cfg.password.value)
  )
  private val tx = new PlayDbTxRunner(db, ec)

  private def insertEvent(conn: Connection): Unit = {
    sql"INSERT INTO shared_event (event_type, payload, created_at) VALUES ('TEST', '{}', NOW(6))"
      .executeUpdate()(conn)
    ()
  }

  private def eventCount(): Int =
    TestDatabase.query("SELECT COUNT(*) FROM shared_event")(_.getInt(1)).head

  override def beforeEach(): Unit = TestDatabase.update("DELETE FROM shared_event")

  override def afterAll(): Unit = {
    db.shutdown()
    pool.shutdown()
  }

  "TxRunner" should {
    "commit when the work returns Right" in {
      val result = Await.result(tx.run[String, Int] { conn => insertEvent(conn); Right(1) }, 10.seconds)
      result shouldBe Right(1)
      eventCount() shouldBe 1
    }

    "roll back when the work returns Left" in {
      val result = Await.result(tx.run[String, Int] { conn => insertEvent(conn); Left("boom") }, 10.seconds)
      result shouldBe Left("boom")
      eventCount() shouldBe 0
    }

    "roll back when the work throws" in {
      val future = tx.run[String, Int] { conn => insertEvent(conn); throw new IllegalStateException("boom") }
      an[IllegalStateException] should be thrownBy Await.result(future, 10.seconds)
      eventCount() shouldBe 0
    }
  }
}