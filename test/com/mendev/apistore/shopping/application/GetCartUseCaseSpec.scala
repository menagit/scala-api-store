package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{Actor, Client}
import com.mendev.apistore.shopping.infrastructure.persistence.RelateCartRepository
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class GetCartUseCaseSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val now = Instant.parse("2026-01-01T10:00:00Z")
  private val actor = Actor("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b07", Client)
  private val user = UUID.fromString(actor.publicId)
  private val cartRepo = new RelateCartRepository

  private val keyboard = CartProduct(1, "Keyboard", available = true, stock = 10, BigDecimal("25.00"), BigDecimal("10.00"), BigDecimal("22.50"))
  private val mouse = CartProduct(2, "Mouse", available = false, stock = 10, BigDecimal("10.00"), BigDecimal("0.00"), BigDecimal("10.00"))
  private val cable = CartProduct(3, "Cable", available = true, stock = 10, BigDecimal("9.99"), BigDecimal("15.00"), BigDecimal("8.49"))

  private val catalog = new ProductCatalog {
    override def findByIds(ids: Seq[Long]): Future[Either[AppError, List[CartProduct]]] =
      Future.successful(Right(List(keyboard, mouse, cable).filter(p => ids.contains(p.id))))
  }

  private val useCase = new GetCartUseCase(catalog, cartRepo, TestDatabase.txRunner)

  override def beforeEach(): Unit = {
    TestDatabase.update("DELETE FROM shopping_cart_item")
    TestDatabase.update("DELETE FROM shopping_cart")
  }

  private def addItem(productId: Long, quantity: Int): Unit =
    Await.result(
      TestDatabase.txRunner.run { conn =>
        cartRepo.getOrCreateCartId(user, now, conn).flatMap(cartId => cartRepo.addQuantity(cartId, productId, quantity, now, conn))
      },
      10.seconds
    )

  private def view(): Either[AppError, CartView] =
    Await.result(useCase.execute(actor), 10.seconds)

  "execute" should {
    "show discounted line totals, mark an unavailable line and leave it out of the total" in {
      addItem(1, 3)
      addItem(2, 2)
      addItem(3, 3)

      view() shouldBe Right(
        CartView(
          List(
            CartLine(1, "Keyboard", 3, BigDecimal("25.00"), BigDecimal("10.00"), BigDecimal("22.50"), BigDecimal("67.50"), available = true),
            CartLine(2, "Mouse", 2, BigDecimal("10.00"), BigDecimal("0.00"), BigDecimal("10.00"), BigDecimal("20.00"), available = false),
            CartLine(3, "Cable", 3, BigDecimal("9.99"), BigDecimal("15.00"), BigDecimal("8.49"), BigDecimal("25.47"), available = true)
          ),
          BigDecimal("92.97")
        )
      )
    }

    "return an empty view with total 0 when there is no cart" in {
      view() shouldBe Right(CartView(Nil, BigDecimal("0.00")))
    }
  }
}
