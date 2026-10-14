package com.mendev.apistore.shared.events

import com.lucidchart.relate.*
import play.api.libs.json.Json

import java.sql.Connection
import java.time.Instant

class RelateEventPublisher extends EventPublisher {

  override def publish(event: DomainEvent, at: Instant, conn: Connection): Unit = {
    val payload = Json.stringify(Json.toJson(event.payload))
    sql"""
      INSERT INTO shared_event (event_type, payload, created_at)
      VALUES (${event.eventType}, $payload, $at)
    """.executeUpdate()(conn)
  }
}