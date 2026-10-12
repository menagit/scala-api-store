package com.mendev.apistore.notifications.application

import java.sql.Connection
import java.time.Instant

trait DeliveryRepository {
  // New rows are PENDING and due at once. A duplicate (event, recipient, template) is a bug and throws
  def insert(eventId: Long, deliveries: List[NewDelivery], at: Instant, conn: Connection): Unit

  // Due PENDING rows, locked and marked SENDING. attempts is the count before this try.
  def claimDue(now: Instant, limit: Int, conn: Connection): List[Delivery]

  def markSent(id: Long, at: Instant, conn: Connection): Unit

  // Back to PENDING with the new attempt count and the time of the next try.
  def markRetry(id: Long, attempts: Int, nextAttemptAt: Instant, at: Instant, conn: Connection): Unit

  def markFailed(id: Long, attempts: Int, at: Instant, conn: Connection): Unit
}