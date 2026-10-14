package com.mendev.apistore.notifications.application

import java.time.Instant

// Notifications own view of the PasswordResetRequested event.
// The JSON shape is the contract with identity (the other context).
final case class PasswordResetRequestedEvent(
  userPublicId: String,
  email: String,
  firstName: String,
  requestedAt: Instant,
  encryptedToken: String
                                            ) {


  override def toString: String = s"PasswordResetRequestedEvent($userPublicId)"
}