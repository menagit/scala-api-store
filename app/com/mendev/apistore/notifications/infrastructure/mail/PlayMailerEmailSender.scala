package com.mendev.apistore.notifications.infrastructure.mail

import com.mendev.apistore.notifications.application.{EmailMessage, EmailSender}
import com.mendev.apistore.shared.error.AppError
import play.api.Logger
import play.api.libs.mailer.{Email, MailerClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class PlayMailerEmailSender(mailer: MailerClient, from: String)(implicit ec: ExecutionContext)
  extends EmailSender {

  private val logger = Logger(getClass)

  override def send(message: EmailMessage): Future[Either[AppError, Unit]] =
    Future[Either[AppError, Unit]] {
      mailer.send(toEmail(message)) // blocking: runs on the pool we were given
      Right(())
    }.recover {
      case NonFatal(exception) =>
        logger.warn(s"Email not sent: ${exception.getClass.getSimpleName}")
        Left(AppError.Unavailable("Email could not be sent"))
    }

  private def toEmail(message: EmailMessage): Email =
    Email(
      subject = message.subject,
      from = from,
      to = Seq(message.to),
      bodyText = Some(message.text),
      bodyHtml = message.html
    )
}