package com.mendev.apistore.notifications.infrastructure.mail

import com.mendev.apistore.notifications.application.{EmailMessage, TemplateRenderer}
import com.mendev.apistore.notifications.infrastructure.mail.views
import com.mendev.apistore.shared.error.{AppError, FieldError}

class TwirlTemplateRenderer extends TemplateRenderer {

  // Template name (the string stored in notifications_delivery) -> how to build the message.
  // A new template is one new line here plus its view files.
  //like a Map<String, Handler> -> Spring
  private val templates: Map[String, (String, Map[String, String]) => Either[AppError, EmailMessage]] =
    Map(
      "welcome" -> welcome,
      "password-changed" -> passwordChanged
    )

  override def render(
                       template: String,
                       recipientEmail: String,
                       data: Map[String, String]
                     ): Either[AppError, EmailMessage] =
    templates.get(template) match {
      case Some(build) => build(recipientEmail, data)
      case None        => Left(AppError.Validation(s"Unknown email template: $template"))
    }

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

  private def required(data: Map[String, String], key: String): Either[AppError, String] =
    data.get(key).toRight(
      AppError.Validation("Missing template data", List(FieldError(key, "required")))
    )
}