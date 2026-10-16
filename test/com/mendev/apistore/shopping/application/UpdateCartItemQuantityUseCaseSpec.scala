package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{Actor, Client}
import com.mendev.apistore.shopping.domain.CartItem
import com.mendev.apistore.shopping.infrastructure.persistence.RelateCartRepository
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Clock, Instant, ZoneOffset}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class UpdateCartItemQuantityUseCaseSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)
  private val actor = Actor("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b07", Client)
  private val user = UUID.fromString(actor.publicId)
  private val cartRepo = new RelateCartRepository

  private val mouse = CartProduct(1, "Mouse", available = true, stock = 4, BigDecimal("10.00"), BigDecimal("0.00"), BigDecimal("10.00"))

  private val catalog = new ProductCatalog {
    override def findByIds(ids: Seq[Long]): Future[Either[AppError, List[CartProduct]]] =
      Future.successful(Right(List(mouse).filter(p => ids.contains(p.id))))
  }

  private val useCase = new UpdateCartItemQuantityUseCase(
    catalog,
    new RemoveFromCartUseCase(cartRepo, TestDatabase.txRunner, clock),
    cartRepo,
    TestDatabase.txRunner,
    clock
  )

  override def beforeEach(): Unit = {
    TestDatabase.update("DELETE FROM shopping_cart_item")
    TestDatabase.update("DELETE FROM shopping_cart")
  }

  private def addItem(productId: Long, quantity: Int): Unit =
    Await.result(
      TestDatabase.txRunner.run { conn =>
        cartRepo.getOrCreateCartId(user, clock.instant(), conn)
          .flatMap(cartId => cartRepo.addQuantity(cartId, productId, quantity, clock.instant(), conn))
      },
      10.seconds
    )

  private def update(productId: Long, quantity: Int): Either[AppError, Unit] =
    Await.result(useCase.execute(actor, UpdateCartItemQuantityCommand(productId, quantity)), 10.seconds)

  private def items(): Either[AppError, List[CartItem]] =
    Await.result(TestDatabase.txRunner.run(conn => cartRepo.findItems(user, conn)), 10.seconds)

  private def cartCount(): Int =
    TestDatabase.query("SELECT COUNT(*) FROM shopping_cart")(_.getInt(1)).head

  "execute" should {
    "set the new quantity" in {
      addItem(1, 2)
      update(1, 3) shouldBe Right(())
      items() shouldBe Right(List(CartItem(1, 3)))
    }

    "delete the line and the cart when the last line is set to 0" in {
      addItem(1, 2)
      update(1, 0) shouldBe Right(())
      items() shouldBe Right(Nil)
      cartCount() shouldBe 0
    }
  }
}
