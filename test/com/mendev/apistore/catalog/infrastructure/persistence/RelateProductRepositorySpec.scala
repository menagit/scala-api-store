package com.mendev.apistore.catalog.infrastructure.persistence

import com.mendev.apistore.catalog.domain.Product
import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.AppError
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration.*

class RelateProductRepositorySpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val repo = new RelateProductRepository

  override def beforeEach(): Unit = TestDatabase.update("DELETE FROM catalog_product")

  private def insertProduct(id: Long, status: String = "ACTIVE", deletedAt: String = "NULL"): Unit =
    TestDatabase.update(
      s"""INSERT INTO catalog_product
         |(id, name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at)
         |VALUES ($id, 'Mouse', 'Wireless mouse', 1, 10.00, 0, 5, '$status', $deletedAt, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""".stripMargin
    )

  "findByIds" should {
    "return the matching rows, including disabled and deleted ones, and skip unknown ids" in {
      insertProduct(1)
      insertProduct(2, status = "DISABLED")
      insertProduct(3, deletedAt = "UTC_TIMESTAMP(6)")
      insertProduct(4)

      val found: Either[AppError, List[Product]] =
        Await.result(TestDatabase.txRunner.run(conn => repo.findByIds(Seq(1L, 2L, 3L, 999L), conn)), 10.seconds)

      found.map(_.map(_.id)).map(_.sorted) shouldBe Right(List(1L, 2L, 3L))
    }
  }
}
