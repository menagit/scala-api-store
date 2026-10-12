package com.mendev.apistore.notifications.application
import java.sql.Connection
import java.time.Instant

trait EventRepository {
  // Oldest first, skipping processed and failed events.
  def findUnprocessed(limit: Int, conn: Connection): List[StoredEvent]

  def markProcessed(id: Long, at: Instant, conn: Connection): Unit

  // The event cannot be handled (a bug, not an outage).
  def markFailed(id: Long, at: Instant, conn: Connection): Unit

}
