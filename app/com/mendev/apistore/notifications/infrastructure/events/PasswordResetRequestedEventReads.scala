package com.mendev.apistore.notifications.infrastructure.events

import com.mendev.apistore.notifications.application.PasswordResetRequestedEvent
import play.api.libs.json.{Json, Reads}

object PasswordResetRequestedEventReads {

  implicit val reads: Reads[PasswordResetRequestedEvent] = Json.reads[PasswordResetRequestedEvent]
}