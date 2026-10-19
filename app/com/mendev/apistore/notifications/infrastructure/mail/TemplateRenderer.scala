package com.mendev.apistore.notifications.infrastructure.mail

import com.mendev.apistore.notifications.application.{EmailMessage, TemplateRenderer}
import com.mendev.apistore.notifications.infrastructure.mail.views
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.TokenCipher

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TwirlTemplateRenderer(cipher: TokenCipher, linkBaseUrl: String) extends TemplateRenderer {

  private type Builder = (String, Map[String, String], Option[String]) => Either[AppError, EmailMessage]

  // Template name (the string stored in notifications_delivery) -> how to build the message.
  // A new template is one new line here plus its view files.
  private val templates: Map[String, Builder] =
    Map(
      "welcome"          -> withoutSecret(welcome),
      "password-changed" -> withoutSecret(passwordChanged),
      "password-reset"   -> passwordReset,
      "order-status"     -> withoutSecret(orderStatus)
    )

  override def render(
                       template: String,
                       recipientEmail: String,
                       data: Map[String, String],
                       secret: Option[String]
                     ): Either[AppError, EmailMessage] =
    templates.get(template) match {
      case Some(build) => build(recipientEmail, data, secret)
      case None        => Left(AppError.Validation(s"Unknown email template: $template"))
    }

  // Most templates need no secret.
  private def withoutSecret(build: (String, Map[String, String]) => Either[AppError, EmailMessage]): Builder =
    (to, data, _) => build(to, data)

  private def welcome(to: String, data: Map[String, String]): Either[AppError, EmailMessage] =
    for {
      name <- required(data, "name")
      link <- required(data, "link")
    } yield EmailMessage(
      to      = to,
      subject = "Welcome to API Store",
      text    = views.txt.welcome(name, link).body,
      html    = Some(views.html.welcome(name, link).body)
    )

  private def passwordChanged(to: String, data: Map[String, String]): Either[AppError, EmailMessage] =
    for {
      name      <- required(data, "name")
      changedAt <- required(data, "changedAt")
    } yield EmailMessage(
      to      = to,
      subject = "Your password was changed",
      text    = views.txt.passwordChanged(name, changedAt).body,
      html    = Some(views.html.passwordChanged(name, changedAt).body)
    )

  private val orderStatusLines: Map[String, String] = Map(
    "CONFIRMED" -> "Your order is confirmed",
    "SHIPPED"   -> "Your order has shipped",
    "DELIVERED" -> "Your order was delivered",
    "CANCELLED" -> "Your order was cancelled"
  )

  private def orderStatus(to: String, data: Map[String, String]): Either[AppError, EmailMessage] =
    required(data, "name").flatMap { name =>
      required(data, "orderId").flatMap { orderId =>
        required(data, "status").flatMap(statusLine).map(line => orderStatusEmail(to, name, orderId, line, data))
      }
    }

  private def statusLine(status: String): Either[AppError, String] =
    orderStatusLines.get(status).toRight(AppError.Validation("Unknown order status", List(FieldError("status", "unknown"))))

  private def orderStatusEmail(to: String, name: String, orderId: String, line: String, data: Map[String, String]): EmailMessage = {
    val tracking = data.get("trackingNumber")
    val eta      = data.get("estimatedDelivery")
    EmailMessage(
      to      = to,
      subject = line,
      text    = views.txt.orderStatus(name, orderId, line, tracking, eta).body,
      html    = Some(views.html.orderStatus(name, orderId, line, tracking, eta).body)
    )
  }

  // The clear token lives only inside this method:
  // decrypted, put in the link, never stored or logged
  private def passwordReset(
                             to: String,
                             data: Map[String, String],
                             secret: Option[String]
                           ): Either[AppError, EmailMessage] =
    for {
      name  <- required(data, "name")
      token <- openSecret(secret)
    } yield resetEmail(to, name, resetLink(token))

  // The clear token lives only in this renderer:
  // decrypted here, put in the link, never stored or logged
  private def openSecret(secret: Option[String]): Either[AppError, String] =
    secret
      .toRight[AppError](AppError.Validation("Missing template data", List(FieldError("secret", "required"))))
      .flatMap(cipher.decrypt)

  private def resetLink(token: String): String =
    s"$linkBaseUrl/reset-password?token=${URLEncoder.encode(token, StandardCharsets.UTF_8)}"

  private def resetEmail(to: String, name: String, link: String): EmailMessage =
    EmailMessage(
      to      = to,
      subject = "Reset your password",
      text    = views.txt.passwordReset(name, link).body,
      html    = Some(views.html.passwordReset(name, link).body)
    )

  private def required(data: Map[String, String], key: String): Either[AppError, String] =
    data.get(key).toRight(
      AppError.Validation("Missing template data", List(FieldError(key, "required")))
    )
}