package com.mendev.apistore.notifications.application

// A delivery claimed for sending
final case class Delivery(
    id: Long,
    eventId: Long,
    recipientUserId: Long,
    recipientEmail: String,
    template: String,
    data: Map[String, String],
    attempts: Int
) {
  override def toString: String = s"Delivery($id, $template, attempts=$attempts)"
}
