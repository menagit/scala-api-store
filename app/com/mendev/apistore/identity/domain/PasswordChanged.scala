package com.mendev.apistore.identity.domain

import com.mendev.apistore.shared.events.DomainEvent

import java.time.Instant

final case class PasswordChanged(userPublicId: String, email: String,
  firstName: String,
  changedAt: Instant
) extends DomainEvent {

  override def eventType: String = "PasswordChanged"

  // Flat strings: the publisher turns this map into JSON.
  // These keys are the contract with notifications.
  override def payload: Map[String, String] = Map(
    "userPublicId" -> userPublicId,
    "email"        -> email,
    "firstName"    -> firstName,
    "changedAt"    -> changedAt.toString
  )

  // Only the id gets printed
  override def toString: String = s"PasswordChanged($userPublicId)"
}