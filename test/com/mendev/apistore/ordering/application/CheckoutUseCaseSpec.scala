package com.mendev.apistore.ordering.application

import com.mendev.apistore.catalog.application.{GetProductSummariesUseCase, TakeStockUseCase}
import com.mendev.apistore.catalog.infrastructure.persistence.{RelateProductRepository, RelateStockRepository}
import com.mendev.apistore.ordering.domain.OrderStatus.Confirmed
import com.mendev.apistore.ordering.infrastructure.cart.InProcessCartSource
import com.mendev.apistore.ordering.infrastructure.catalog.InProcessStockReservation
import com.mendev.apistore.ordering.infrastructure.persistence.{RelateOrderRepository, RelateOrderStatusHistoryRepository}
import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.events.RelateEventPublisher
import com.mendev.apistore.shared.security.{Actor, Client}
import com.mendev.apistore.shopping.application.{ClearCartUseCase, LockCartItemsUseCase}
import com.mendev.apistore.shopping.infrastructure.persistence.RelateCartRepository
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.sql.Connection
import java.time.{Clock, Instant, ZoneOffset}
import java.util.UUID
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*

class CheckoutUseCaseSpec extends AnyWordSpec with Matchers with EitherValues with BeforeAndAfterEach {

  private val now     = Instant.parse("2026-01-01T10:00:00Z")
  private val user    = UUID.fromString("01900000-0000-7000-8000-000000000001")
  private val orderId = UUID.fromString("01900000-0000-7000-8000-0000000000aa")
  private val actor   = Actor(user.toString, Client)
  private val cartIsEmpty = Left(AppError.Validation("Cart is empty"))

  private val cartRepo = new RelateCartRepository

  private val checkout = new CheckoutUseCase(
    new InProcessCartSource(new LockCartItemsUseCase(cartRepo), new ClearCartUseCase(cartRepo)),
    new InProcessStockReservation(
      new GetProductSummariesUseCase(new RelateProductRepository, TestDatabase.txRunner),
      new TakeStockUseCase(new RelateStockRepository)
    ),
    new RelateOrderRepository,
    new RelateOrderStatusHistoryRepository,
    new RelateEventPublisher,
    () => orderId,
    TestDatabase.txRunner,
    Clock.fixed(now, ZoneOffset.UTC)
  )

  override def beforeEach(): Unit =
    List(
      "ordering_order_status_history",
      "ordering_order_item",
      "ordering_order",
      "shared_event",
      "shopping_cart_item",
      "shopping_cart",
      "catalog_product"
    ).foreach(table => TestDatabase.update(s"DELETE FROM $table"))

  private def insertProduct(id: Long, stock: Int, price: String = "100.00", discount: String = "10", status: String = "ACTIVE"): Unit =
    TestDatabase.update(
      s"""INSERT INTO catalog_product
         |(id, name, description, category_id, price, discount_percent, stock, status, deleted_at, created_at, updated_at)
         |VALUES ($id, 'Product $id', 'Description', 1, $price, $discount, $stock, '$status', NULL, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))""".stripMargin
    )

  private def addToCart(productId: Long, quantity: Int): Unit =
    run(conn => cartRepo.getOrCreateCartId(user, now, conn).flatMap(cartId => cartRepo.addQuantity(cartId, productId, quantity, now, conn)))

  private def run[A](work: Connection => Either[AppError, A]): A =
    Await.result(TestDatabase.txRunner.run(work), 10.seconds).value

  private def await[A](result: Future[A]): A = Await.result(result, 10.seconds)

  private def count(table: String): Int =
    TestDatabase.query(s"SELECT COUNT(*) FROM $table")(_.getInt(1)).head

  private def stockOf(id: Long): Int =
    TestDatabase.query(s"SELECT stock FROM catalog_product WHERE id = $id")(_.getInt(1)).head

  "checkout" should {
    "turn a valid cart into a confirmed order" in {
      insertProduct(1, stock = 5)
      insertProduct(2, stock = 3, price = "19.99", discount = "0")
      addToCart(2, 1)
      addToCart(1, 2)

      val order = await(checkout.execute(actor)).value

      order.publicId shouldBe orderId
      order.status shouldBe Confirmed
      order.total shouldBe BigDecimal("199.99")
      order.items.map(i => (i.productId, i.discountedUnitPrice, i.quantity, i.lineTotal)) shouldBe List(
        (1L, BigDecimal("90.00"), 2, BigDecimal("180.00")),
        (2L, BigDecimal("19.99"), 1, BigDecimal("19.99"))
      )
      stockOf(1) shouldBe 3
      stockOf(2) shouldBe 2
      count("shopping_cart") shouldBe 0
      count("shopping_cart_item") shouldBe 0

      TestDatabase.query("SELECT old_status, new_status, changed_by_type FROM ordering_order_status_history ORDER BY id") { rs =>
        (Option(rs.getString(1)), rs.getString(2), rs.getString(3))
      } shouldBe List((None, "PENDING", "CLIENT"), (Some("PENDING"), "CONFIRMED", "SYSTEM"))

      val events = TestDatabase.query("SELECT payload FROM shared_event WHERE event_type = 'OrderStatusChanged' ORDER BY id")(rs => Json.parse(rs.getString(1)))
      events.map(e => ((e \ "from").asOpt[String], (e \ "to").as[String])) shouldBe List((None, "PENDING"), (Some("PENDING"), "CONFIRMED"))
    }

    "refuse no cart and an empty cart" in {
      await(checkout.execute(actor)) shouldBe cartIsEmpty

      run(conn => cartRepo.getOrCreateCartId(user, now, conn))
      await(checkout.execute(actor)) shouldBe cartIsEmpty

      count("ordering_order") shouldBe 0
    }

    "refuse the whole cart and name every product that cannot be bought" in {
      insertProduct(1, stock = 1)
      insertProduct(2, stock = 5, status = "DISABLED")
      addToCart(1, 2)
      addToCart(2, 1)

      await(checkout.execute(actor)) shouldBe Left(
        AppError.Conflict("Cannot check out: product 1 has only 1 in stock; product 2 is unavailable")
      )
      stockOf(1) shouldBe 1
      stockOf(2) shouldBe 5
      count("shopping_cart_item") shouldBe 2
      count("ordering_order") shouldBe 0
    }

    "create only one order when the same cart is checked out twice at once" in {
      insertProduct(1, stock = 5)
      addToCart(1, 1)

      val first   = checkout.execute(actor)
      val second  = checkout.execute(actor)
      val results = List(await(first), await(second))

      results.collect { case Right(order) => order.status } shouldBe List(Confirmed)
      results.collect { case Left(error) => error } shouldBe List(AppError.Validation("Cart is empty"))
      count("ordering_order") shouldBe 1
      stockOf(1) shouldBe 4
    }
  }
}
