package com.mendev.apistore.identity.domain

import com.mendev.apistore.shared.events.DomainEvent

import java.time.Instant

final case  class PasswordResetRequested (
  userPublicId: String,
  email: String,
  firstName: String,
  requestedAt: Instant,
  encryptedToken: String
                                         )
  extends DomainEvent{

  override def eventType: String = "PasswordResetRequested"

  override def payload: Map[String, String] = Map(
    "userPublicId" -> userPublicId,
    "email"        -> email,
    "firstName"    -> firstName,
    "requestedAt"    -> requestedAt.toString,
    "encryptedToken"  -> encryptedToken
  )

  override def toString: String = s"PasswordResetRequested($userPublicId)"

}
