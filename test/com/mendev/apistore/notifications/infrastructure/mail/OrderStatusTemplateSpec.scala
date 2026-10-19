package com.mendev.apistore.notifications.infrastructure.mail

import com.mendev.apistore.shared.config.Secret
import com.mendev.apistore.shared.security.TokenCipher
import org.scalatest.{EitherValues, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OrderStatusTemplateSpec extends AnyWordSpec with Matchers with EitherValues with OptionValues {

  private val renderer =
    new TwirlTemplateRenderer(new TokenCipher(Secret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")), "http://localhost:3000")

  private val orderId = "0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b99"

  "The order-status template" should {
    "show the shipped subject, name, order id, tracking number and estimated delivery in text and HTML" in {
      val data = Map(
        "name"              -> "Ana",
        "orderId"           -> orderId,
        "status"            -> "SHIPPED",
        "trackingNumber"    -> "TRK-123",
        "estimatedDelivery" -> "2026-10-25"
      )

      val email = renderer.render("order-status", "ana@example.test", data, None).value

      email.subject shouldBe "Your order has shipped"
      List(email.text, email.html.value).foreach { body =>
        body should include("Your order has shipped")
        body should include("Hi, Ana")
        body should include(orderId)
        body should include("Tracking number: TRK-123")
        body should include("Estimated delivery: 2026-10-25")
      }
    }
  }
}
