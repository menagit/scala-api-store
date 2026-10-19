package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

trait EventHandler[E] {
  //One handler per event type. It decides who gets which email and nothing more: no sending, no SQL of its own; it reads only through ports.
  //A Left means this event cannot be handled
  def handle(event: E, conn: Connection): Either[AppError, List[NewDelivery]]
}
