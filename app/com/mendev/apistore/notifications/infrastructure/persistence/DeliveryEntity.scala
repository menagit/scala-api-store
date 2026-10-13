package com.mendev.apistore.notifications.infrastructure.persistence

import com.lucidchart.relate.SqlRow
import com.mendev.apistore.notifications.application.{Delivery, NewDelivery}
import com.mendev.apistore.notifications.domain.DeliveryStatus
import play.api.libs.json.Json

import java.time.Instant

final case class DeliveryEntity(
   id: Long,
   eventId: Long,
   recipientPublicId: String,
   recipientEmail: String,
   template: String,
   data: String, // JSON text
   status: String,
   attempts: Int,
   nextAttemptAt: Instant,
   createdAt: Instant,
   updatedAt: Instant
                               ) {

  override def toString: String = s"DeliveryEntity($id, $template)"
}

object DeliveryEntity {

  // A new row has no id yet. The database assigns it, so the insert does not use this so O.
  def fromNew(eventId: Long, delivery: NewDelivery, at: Instant): DeliveryEntity =
    DeliveryEntity(
      id = 0L,
      eventId = eventId,
      recipientPublicId = delivery.recipientPublicId,
      recipientEmail = delivery.recipientEmail,
      template = delivery.template,
      data = Json.stringify(Json.toJson(delivery.data)),
      status = DeliveryStatus.Pending.value,
      attempts = 0,
      nextAttemptAt = at, // due at once
      createdAt = at,
      updatedAt = at
    )

  def toDomain(entity: DeliveryEntity): Delivery =
    Delivery(
      id = entity.id,
      eventId = entity.eventId,
      recipientPublicId = entity.recipientPublicId,
      recipientEmail = entity.recipientEmail,
      template = entity.template,
      data = parseData(entity),
      attempts = entity.attempts
    )

  def fromRow(row: SqlRow): DeliveryEntity =
    DeliveryEntity(
      id = row.long("id"),
      eventId = row.long("event_id"),
      recipientPublicId = row.string("recipient_public_id"),
      recipientEmail = row.string("recipient_email"),
      template = row.string("template"),
      data = row.string("data"),
      status = row.string("status"),
      attempts = row.int("attempts"),
      nextAttemptAt = row.instant("next_attempt_at"),
      createdAt = row.instant("created_at"),
      updatedAt = row.instant("updated_at")
    )

  // A bad shape Json its a bug
  private def parseData(entity: DeliveryEntity): Map[String, String] =
    Json.parse(entity.data).asOpt[Map[String, String]]
      .getOrElse(throw new IllegalStateException(s"Delivery ${entity.id}: data is not a string map"))
}