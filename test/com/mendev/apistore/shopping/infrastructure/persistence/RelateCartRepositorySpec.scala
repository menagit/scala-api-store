package com.mendev.apistore.shopping.infrastructure.persistence

import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shopping.domain.CartItem
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.sql.Connection
import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.*

class RelateCartRepositorySpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val repo = new RelateCartRepository
  private val now = Instant.parse("2026-01-01T10:00:00Z")
  private val ana = UUID.fromString("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b07")
  private val bob = UUID.fromString("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b08")

  override def beforeEach(): Unit = {
    TestDatabase.update("DELETE FROM shopping_cart_item")
    TestDatabase.update("DELETE FROM shopping_cart")
  }

  private def run[A](work: Connection => Either[AppError, A]): Either[AppError, A] =
    Await.result(TestDatabase.txRunner.run(work), 10.seconds)

  private def addItem(user: UUID, productId: Long, quantity: Int): Either[AppError, Unit] =
    run(conn => repo.getOrCreateCartId(user, now, conn).flatMap(cartId => repo.addQuantity(cartId, productId, quantity, now, conn)))

  private def cartCount(): Int =
    TestDatabase.query("SELECT COUNT(*) FROM shopping_cart")(_.getInt(1)).head

  "getOrCreateCartId" should {
    "return the same id twice for the same user and keep one row" in {
      val first = run(conn => repo.getOrCreateCartId(ana, now, conn))
      val second = run(conn => repo.getOrCreateCartId(ana, now, conn))
      second shouldBe first
      cartCount() shouldBe 1
    }
  }

  "addQuantity" should {
    "add up the quantity on one line for the same product" in {
      addItem(ana, 1, 2) shouldBe Right(())
      addItem(ana, 1, 3) shouldBe Right(())
      run(conn => repo.findItems(ana, conn)) shouldBe Right(List(CartItem(1, 5)))
    }
  }

  "findItems" should {
    "return only that user's lines, in the order added" in {
      addItem(ana, 2, 1)
      addItem(ana, 1, 3)
      addItem(bob, 3, 4)
      run(conn => repo.findItems(ana, conn)) shouldBe Right(List(CartItem(2, 1), CartItem(1, 3)))
    }
  }
}
