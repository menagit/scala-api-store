package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{Actor, Client}
import com.mendev.apistore.shopping.infrastructure.persistence.RelateCartRepository
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Clock, Instant, ZoneOffset}
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.*

class RemoveFromCartUseCaseSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)
  private val actor = Actor("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b07", Client)
  private val cartRepo = new RelateCartRepository
  private val useCase = new RemoveFromCartUseCase(cartRepo, TestDatabase.txRunner, clock)

  override def beforeEach(): Unit = {
    TestDatabase.update("DELETE FROM shopping_cart_item")
    TestDatabase.update("DELETE FROM shopping_cart")
  }

  private def addItem(productId: Long, quantity: Int): Unit =
    Await.result(
      TestDatabase.txRunner.run { conn =>
        cartRepo.getOrCreateCartId(UUID.fromString(actor.publicId), clock.instant(), conn)
          .flatMap(cartId => cartRepo.addQuantity(cartId, productId, quantity, clock.instant(), conn))
      },
      10.seconds
    )

  private def remove(productId: Long): Either[AppError, Unit] =
    Await.result(useCase.execute(actor, productId), 10.seconds)

  private def count(table: String): Int =
    TestDatabase.query(s"SELECT COUNT(*) FROM $table")(_.getInt(1)).head

  "execute" should {
    "delete the line and the cart when the last line is removed" in {
      addItem(1, 2)
      remove(1) shouldBe Right(())
      count("shopping_cart_item") shouldBe 0
      count("shopping_cart") shouldBe 0
    }

    "return NotFound when there is no cart" in {
      remove(1) shouldBe Left(AppError.NotFound("Product not in cart"))
    }
  }
}
