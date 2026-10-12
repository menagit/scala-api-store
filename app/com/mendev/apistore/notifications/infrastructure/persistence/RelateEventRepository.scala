package com.mendev.apistore.notifications.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.notifications.application.{EventRepository, StoredEvent}

import java.sql.Connection
import java.time.Instant

class RelateEventRepository extends EventRepository {

  // Oldest first. The rows stay locked until the caller's transaction ends.
  // and the failures are skipped
  override def findUnprocessed(limit: Int, conn: Connection): List[StoredEvent] =
    sql"""
      SELECT id, event_type, payload, created_at, processed_at, failed_at
      FROM shared_event
      WHERE processed_at IS NULL AND failed_at IS NULL
      ORDER BY id
      LIMIT $limit
      FOR UPDATE SKIP LOCKED
    """.asList(EventEntity.fromRow)(conn).map(EventEntity.toDomain)

  override def markProcessed(id: Long, at: Instant, conn: Connection): Unit = {
    val updated = sql"""
      UPDATE shared_event SET processed_at = $at WHERE id = $id
    """.executeUpdate()(conn)

   //No read then it should be a Bug
    if (updated != 1)
      throw new IllegalStateException(s"Event $id not found: not marked processed")
  }

  override def markFailed(id: Long, at: Instant, conn: Connection): Unit = {
    val updated = sql"""
      UPDATE shared_event SET failed_at = $at WHERE id = $id
    """.executeUpdate()(conn)

    if (updated != 1)
      throw new IllegalStateException(s"Event $id not found: not marked failed")
  }
}