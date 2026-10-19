package com.mendev.apistore.notifications.infrastructure.events

import com.mendev.apistore.notifications.application.OrderStatusChangedEvent
import play.api.libs.json.{Json, Reads}

object OrderStatusChangedEventReads {

  implicit val reads: Reads[OrderStatusChangedEvent] = Json.reads[OrderStatusChangedEvent]
}
