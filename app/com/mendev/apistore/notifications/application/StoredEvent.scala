package com.mendev.apistore.notifications.application

//One row from the outbox table, leaving out personal data in the toString
final case class StoredEvent(id: Long, eventType: String, payload: String) {
  override def toString: String = s"StoredEvent($id, $eventType)"
}
