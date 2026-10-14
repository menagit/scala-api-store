package com.mendev.apistore.notifications.application

import java.time.Instant

// Notifications own view of the PasswordChanged event.
// The JSON shape is the contract with identity ->  no class is shared.
final case class PasswordChangedEvent(
   userPublicId: String,
   email: String,
   firstName: String,
   changedAt: Instant
) {

  override def toString: String = s"PasswordChangedEvent($userPublicId)"
}