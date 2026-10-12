package com.mendev.apistore.notifications.infrastructure.persistence

import com.lucidchart.relate.*
import com.mendev.apistore.notifications.application.{Delivery, DeliveryRepository, NewDelivery}
import com.mendev.apistore.notifications.domain.DeliveryStatus

import java.sql.Connection
import java.time.Instant

class RelateDeliveryRepository extends DeliveryRepository {

  override def insert(eventId: Long, deliveries: List[NewDelivery], at: Instant, conn: Connection): Unit =
    deliveries.foreach { delivery =>
      val entity = DeliveryEntity.fromNew(eventId, delivery, at)
      sql"""
        INSERT INTO notifications_delivery
          (event_id, recipient_user_id, recipient_email, template, data, status, attempts,
           next_attempt_at, created_at, updated_at)
        VALUES
          (${entity.eventId}, ${entity.recipientUserId}, ${entity.recipientEmail}, ${entity.template},
           ${entity.data}, ${entity.status}, ${entity.attempts},
           ${entity.nextAttemptAt}, ${entity.createdAt}, ${entity.updatedAt})
      """.executeUpdate()(conn)
    }

  override def claimDue(now: Instant, limit: Int, conn: Connection): List[Delivery] = {
    val pending = DeliveryStatus.Pending.value
    val sending = DeliveryStatus.Sending.value

    val due = sql"""
      SELECT id, event_id, recipient_user_id, recipient_email, template, data, status, attempts,
             next_attempt_at, created_at, updated_at
      FROM notifications_delivery
      WHERE status = $pending AND next_attempt_at <= $now
      ORDER BY next_attempt_at, id
      LIMIT $limit
      FOR UPDATE SKIP LOCKED
    """.asList(DeliveryEntity.fromRow)(conn)

    // At most `limit` rows, so one update per row is simple enough.
    due.foreach { entity =>
      sql"""
        UPDATE notifications_delivery SET status = $sending, updated_at = $now WHERE id = ${entity.id}
      """.executeUpdate()(conn)
    }

    due.map(DeliveryEntity.toDomain)
  }

  override def markSent(id: Long, at: Instant, conn: Connection): Unit = {
    val sent    = DeliveryStatus.Sent.value
    val sending = DeliveryStatus.Sending.value
    val updated = sql"""
      UPDATE notifications_delivery SET status = $sent, updated_at = $at
      WHERE id = $id AND status = $sending
    """.executeUpdate()(conn)
    expectOne(updated, id)
  }

  override def markRetry(id: Long, attempts: Int, nextAttemptAt: Instant, at: Instant, conn: Connection): Unit = {
    val pending = DeliveryStatus.Pending.value
    val sending = DeliveryStatus.Sending.value
    val updated = sql"""
      UPDATE notifications_delivery
      SET status = $pending, attempts = $attempts, next_attempt_at = $nextAttemptAt, updated_at = $at
      WHERE id = $id AND status = $sending
    """.executeUpdate()(conn)
    expectOne(updated, id)
  }

  override def markFailed(id: Long, attempts: Int, at: Instant, conn: Connection): Unit = {
    val failed  = DeliveryStatus.Failed.value
    val sending = DeliveryStatus.Sending.value
    val updated = sql"""
      UPDATE notifications_delivery SET status = $failed, attempts = $attempts, updated_at = $at
      WHERE id = $id AND status = $sending
    """.executeUpdate()(conn)
    expectOne(updated, id)
  }

  // Only a SENDING row can be finished.Otherwise, it's a bug
  private def expectOne(updated: Int, id: Long): Unit =
    if (updated != 1)
      throw new IllegalStateException(s"Delivery $id is not SENDING: not updated")
}