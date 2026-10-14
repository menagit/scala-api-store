package com.mendev.apistore.notifications.application

// A delivery claimed for sending
final case class Delivery(
    id: Long,
    eventId: Long,
    recipientPublicId: String,
    recipientEmail: String,
    template: String,
    data: Map[String, String],
    secret: Option[String], // encrypted
    attempts: Int
) {
  override def toString: String = s"Delivery($id, $template, attempts=$attempts)"
}
