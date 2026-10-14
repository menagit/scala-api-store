package com.mendev.apistore.shared.events

import java.sql.Connection
import java.time.Instant

trait EventPublisher {
  // Writes into the outbox on the caller's connection, so the event
  // commits or rolls back together with the change.
  // An SQL error is thrown, and TxRunner rolls the whole transaction back.
  def publish(event: DomainEvent, at: Instant, conn: Connection): Unit
}