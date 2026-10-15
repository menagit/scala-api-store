package com.mendev.apistore.catalog.infrastructure.persistence

import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.AppError
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.sql.Connection
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.*

class RelateStockRepositorySpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val repo = new RelateStockRepository
  private val now = Instant.parse("2026-01-01T10:00:00Z")
  private val conflict = Left(AppError.Conflict("Product unavailable or insufficient stock"))

  override def beforeEach(): Unit = TestDatabase.update("DELETE FROM catalog_product")

  private def insertProduct(id: Long, stock: Int, status: String = "ACTIVE", deletedAt: String = "NULL"): Unit =
    TestDatabase.update(
      s"""INSERT INTO catalog_product
         |(id, name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at)
         |VALUES ($id, 'Mouse', 'Wireless mouse', 1, 10.00, 0, $stock, '$status', $deletedAt, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""".stripMargin
    )

  private def stockOf(id: Long): Int =
    TestDatabase.query(s"SELECT stock FROM catalog_product WHERE id = $id")(_.getInt(1)).head

  private def updatedAtOf(id: Long): String =
    TestDatabase.query(s"SELECT DATE_FORMAT(updated_at, '%Y-%m-%dT%H:%i:%sZ') FROM catalog_product WHERE id = $id")(_.getString(1)).head

  private def run(work: Connection => Either[AppError, Unit]): Either[AppError, Unit] =
    Await.result(TestDatabase.txRunner.run(work), 10.seconds)

  "decrement" should {
    "take the quantity off when there is enough stock" in {
      insertProduct(1, stock = 5)
      run(conn => repo.decrement(1, 2, now, conn)) shouldBe Right(())
      stockOf(1) shouldBe 3
      updatedAtOf(1) shouldBe now.toString
    }

    "take the exact stock left down to zero" in {
      insertProduct(1, stock = 3)
      run(conn => repo.decrement(1, 3, now, conn)) shouldBe Right(())
      stockOf(1) shouldBe 0
    }

    "return Conflict when there is not enough stock" in {
      insertProduct(1, stock = 2)
      run(conn => repo.decrement(1, 3, now, conn)) shouldBe conflict
      stockOf(1) shouldBe 2
    }

    "return Conflict for a disabled product" in {
      insertProduct(1, stock = 5, status = "DISABLED")
      run(conn => repo.decrement(1, 1, now, conn)) shouldBe conflict
      stockOf(1) shouldBe 5
    }

    "return Conflict for a deleted product" in {
      insertProduct(1, stock = 5, deletedAt = "UTC_TIMESTAMP(6)")
      run(conn => repo.decrement(1, 1, now, conn)) shouldBe conflict
      stockOf(1) shouldBe 5
    }

    "return Conflict for an unknown id" in {
      run(conn => repo.decrement(999, 1, now, conn)) shouldBe conflict
    }

    "let only one of two buyers take the last unit" in {
      insertProduct(1, stock = 1)
      val first = TestDatabase.txRunner.run(conn => repo.decrement(1, 1, now, conn))
      val second = TestDatabase.txRunner.run(conn => repo.decrement(1, 1, now, conn))
      val results = List(Await.result(first, 10.seconds), Await.result(second, 10.seconds))
      results should contain theSameElementsAs List(Right(()), conflict)
      stockOf(1) shouldBe 0
    }
  }

  "increment" should {
    "add the quantity" in {
      insertProduct(1, stock = 3)
      run(conn => repo.increment(1, 2, now, conn)) shouldBe Right(())
      stockOf(1) shouldBe 5
      updatedAtOf(1) shouldBe now.toString
    }

    "work on a disabled and on a deleted product" in {
      insertProduct(1, stock = 3, status = "DISABLED")
      insertProduct(2, stock = 3, deletedAt = "UTC_TIMESTAMP(6)")
      run(conn => repo.increment(1, 2, now, conn)) shouldBe Right(())
      run(conn => repo.increment(2, 2, now, conn)) shouldBe Right(())
      stockOf(1) shouldBe 5
      stockOf(2) shouldBe 5
    }

    "return NotFound for an unknown id" in {
      run(conn => repo.increment(999, 1, now, conn)) shouldBe Left(AppError.NotFound("Product not found"))
    }
  }
}
