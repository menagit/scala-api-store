package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

trait RecipientDirectory {
  def find(userPublicId: String, conn: Connection): Either[AppError, Option[Recipient]]
}
