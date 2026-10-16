package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.{AppError, FieldError}
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

class AddToCartUseCaseSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)
  private val actor = Actor("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b07", Client)
  private val cartRepo = new RelateCartRepository

  private val mouse = CartProduct(1, "Mouse", available = true, stock = 4, BigDecimal("10.00"), BigDecimal("0.00"), BigDecimal("10.00"))
  private val hidden = mouse.copy(id = 2, available = false)

  private val catalog = new ProductCatalog {
    override def findByIds(ids: Seq[Long]): Future[Either[AppError, List[CartProduct]]] =
      Future.successful(Right(List(mouse, hidden).filter(p => ids.contains(p.id))))
  }

  private val useCase = new AddToCartUseCase(catalog, cartRepo, TestDatabase.txRunner, clock)

  override def beforeEach(): Unit = {
    TestDatabase.update("DELETE FROM shopping_cart_item")
    TestDatabase.update("DELETE FROM shopping_cart")
  }

  private def add(productId: Long, quantity: Int): Either[AppError, Unit] =
    Await.result(useCase.execute(actor, AddToCartCommand(productId, quantity)), 10.seconds)

  private def items(): Either[AppError, List[CartItem]] =
    Await.result(TestDatabase.txRunner.run(conn => cartRepo.findItems(UUID.fromString(actor.publicId), conn)), 10.seconds)

  "execute" should {
    "add a new line, then raise its quantity when the same product is added again" in {
      add(1, 1) shouldBe Right(())
      add(1, 2) shouldBe Right(())
      items() shouldBe Right(List(CartItem(1, 3)))
    }

    "return NotFound for an unavailable product" in {
      add(2, 1) shouldBe Left(AppError.NotFound("Product not found"))
      items() shouldBe Right(Nil)
    }

    "return NotFound for an unknown product" in {
      add(999, 1) shouldBe Left(AppError.NotFound("Product not found"))
    }

    "count what is already in the cart against the stock" in {
      add(1, 3) shouldBe Right(())
      add(1, 2) shouldBe Left(AppError.Conflict("Not enough stock"))
      items() shouldBe Right(List(CartItem(1, 3)))
    }

    "refuse a quantity of 0" in {
      add(1, 0) shouldBe Left(AppError.Validation("Invalid request", List(FieldError("quantity", "must be greater than 0"))))
    }
  }
}
