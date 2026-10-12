package com.mendev.apistore.notifications.application

//What the handler needs.
final case class NewDelivery(
    recipientUserId: Long,
    recipientEmail: String,
    template: String,
    data: Map[String, String]
) {
  override def toString: String = s"NewDelivery($recipientUserId, $template)"
}
