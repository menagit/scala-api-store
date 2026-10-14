package com.mendev.apistore.notifications.infrastructure.events

import com.mendev.apistore.notifications.application.PasswordChangedEvent
import play.api.libs.json.{Json, Reads}

object PasswordChangedEventReads {

  implicit val reads: Reads[PasswordChangedEvent] = Json.reads[PasswordChangedEvent]
}