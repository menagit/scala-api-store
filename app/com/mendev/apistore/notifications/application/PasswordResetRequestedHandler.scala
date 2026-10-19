package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class PasswordResetRequestedHandler extends EventHandler[PasswordResetRequestedEvent] {

  override def handle(event: PasswordResetRequestedEvent, conn: Connection): Either[AppError, List[NewDelivery]] =
    Right(
      List(
        NewDelivery(
          recipientPublicId = event.userPublicId,
          recipientEmail    = event.email,
          template          = PasswordResetRequestedHandler.Template,
          data              = Map("name" -> event.firstName),
          // Still encrypted. Only the renderer opens it, when it builds the link.
          secret            = Some(event.encryptedToken)
        )
      )
    )
}

object PasswordResetRequestedHandler {
  val Template = "password-reset"
}