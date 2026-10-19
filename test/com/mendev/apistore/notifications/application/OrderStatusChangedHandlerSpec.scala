package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.lang.reflect.Proxy
import java.sql.Connection

class OrderStatusChangedHandlerSpec extends AnyWordSpec with Matchers {

  private val userId  = "0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b07"
  private val orderId = "0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b99"

  private val conn: Connection =
    Proxy
      .newProxyInstance(getClass.getClassLoader, Array(classOf[Connection]), (_, _, _) => throw new UnsupportedOperationException)
      .asInstanceOf[Connection]

  private val directory: RecipientDirectory =
    (publicId, _) => Right(Option.when(publicId == userId)(Recipient("ana@example.test", "Ana")))

  private val handler = new OrderStatusChangedHandler(directory)

  private def event(to: String, user: String): OrderStatusChangedEvent =
    OrderStatusChangedEvent(orderId, user, to, None, None)

  "The order status changed handler" should {
    "send one order-status email for a CONFIRMED order and none for a PENDING one" in {
      handler.handle(event("CONFIRMED", userId), conn) shouldBe Right(
        List(NewDelivery(userId, "ana@example.test", "order-status", Map("name" -> "Ana", "orderId" -> orderId, "status" -> "CONFIRMED")))
      )
      handler.handle(event("PENDING", userId), conn) shouldBe Right(Nil)
    }

    "refuse an event whose recipient is unknown" in {
      handler.handle(event("CONFIRMED", "0192f1c8-0000-7000-8000-000000000000"), conn) shouldBe Left(AppError.NotFound("Recipient not found"))
    }
  }
}
