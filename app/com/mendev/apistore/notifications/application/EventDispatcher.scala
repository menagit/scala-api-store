package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

//Finds the actual handler for the event type and runs it
trait EventDispatcher {
  // None: no handler for this event type.
  // Some(Left): the payload could not be read, or the handler refused the event.
  def fanOut(event: StoredEvent, conn: Connection): Option[Either[AppError, List[NewDelivery]]]
}
