package com.mendev.apistore.notifications.infrastructure.persistence

import com.lucidchart.relate.SqlRow
import com.mendev.apistore.notifications.application.StoredEvent

import java.time.Instant

final case class EventEntity(
                              id: Long,
                              eventType: String,
                              payload: String,
                              createdAt: Instant,
                              processedAt: Option[Instant],
                              failedAt: Option[Instant]
                            ) {
  // Same override to not print personal data
  override def toString: String = s"EventEntity($id, $eventType)"
}

object EventEntity {


  def toDomain(entity: EventEntity): StoredEvent =
    StoredEvent(
      id = entity.id,
      eventType = entity.eventType,
      payload = entity.payload
    )

  def fromRow(row: SqlRow): EventEntity =
    EventEntity(
      id = row.long("id"),
      eventType = row.string("event_type"),
      payload = row.string("payload"),
      createdAt = row.instant("created_at"),
      processedAt = row.instantOption("processed_at"),
      failedAt = row.instantOption("failed_at")
    )
}