package com.mendev.apistore.ordering.domain

import com.mendev.apistore.ordering.domain.OrderStatus.{Cancelled, Confirmed, Delivered, Pending, Shipped}
import com.mendev.apistore.shared.error.{AppError, FieldError}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Instant, LocalDate}
import java.util.UUID

class OrderSpec extends AnyWordSpec with Matchers {

  private val now      = Instant.parse("2026-01-01T10:00:00Z")
  private val client   = ChangedBy.Client(UUID.fromString("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b07"))
  private val manager  = ChangedBy.Manager(UUID.fromString("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b08"))
  private val tracking = TrackingNumber.from("TRACK-123").toOption
  private val eta      = Some(LocalDate.parse("2026-01-05"))

  private def order(status: OrderStatus): Order =
    Order(
      id = 1,
      publicId = UUID.fromString("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b09"),
      userPublicId = client.publicId,
      status = status,
      total = BigDecimal("10.00"),
      trackingNumber = None,
      estimatedDelivery = None,
      items = Nil,
      createdAt = now.minusSeconds(60),
      updatedAt = now.minusSeconds(60)
    )

  "changeStatus" should {
    "accept every valid move made by an allowed actor" in {
      val moves = List(
        (Pending, Confirmed, ChangedBy.System),
        (Pending, Cancelled, client),
        (Confirmed, Shipped, manager),
        (Confirmed, Cancelled, manager),
        (Shipped, Delivered, manager)
      )
      moves.foreach { case (from, to, by) =>
        order(from).changeStatus(to, by, tracking, eta, now).map { case (changed, entry) => (changed.status, entry) } shouldBe
          Right((to, OrderStatusHistoryEntry.changed(from, to, by, now)))
      }
    }

    "refuse invalid moves with CONFLICT" in {
      val moves = List(Pending, Confirmed, Shipped, Delivered, Cancelled).map(to => (Delivered, to)) ++
        List((Shipped, Cancelled), (Pending, Shipped))
      moves.foreach { case (from, to) =>
        order(from).changeStatus(to, manager, tracking, eta, now).left.map(_.code) shouldBe Left("CONFLICT")
      }
    }

    "refuse the wrong actor with FORBIDDEN" in {
      order(Pending).changeStatus(Confirmed, manager, None, None, now) shouldBe Left(AppError.Forbidden())
      order(Pending).changeStatus(Confirmed, client, None, None, now) shouldBe Left(AppError.Forbidden())
      order(Confirmed).changeStatus(Shipped, client, tracking, eta, now) shouldBe Left(AppError.Forbidden())
    }

    "refuse SHIPPED without a tracking number or an estimated delivery date" in {
      order(Confirmed).changeStatus(Shipped, manager, None, None, now) shouldBe Left(
        AppError.Validation(
          "Invalid request",
          List(FieldError("trackingNumber", "is required"), FieldError("estimatedDelivery", "is required"))
        )
      )
    }
  }
}
