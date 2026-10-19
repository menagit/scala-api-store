package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class PasswordChangedHandler extends EventHandler[PasswordChangedEvent] {

  override def handle(event: PasswordChangedEvent, conn: Connection): Either[AppError, List[NewDelivery]] =
    Right(
      List(
        NewDelivery(
          recipientPublicId = event.userPublicId,
          recipientEmail    = event.email,
          template          = PasswordChangedHandler.Template,
          data = Map(
            "name"      -> event.firstName,
            "changedAt" -> PasswordChangedHandler.TimeFormat.format(event.changedAt)
          )
        )
      )
    )
}

object PasswordChangedHandler {
  val Template = "password-changed"

  // Fixed pattern in UTC
  private val TimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC)
}