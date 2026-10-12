package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError

trait EventHandler[E] {
  //One handler per event type. It decides who gets which email and nothing more: no sending, no SQL.
  //A Left means this event cannot be handled
  def handle(event: E): Either[AppError, List[NewDelivery]]
}
