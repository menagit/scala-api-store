package com.mendev.apistore.notifications.application

//What the handler needs.
final case class NewDelivery(
    recipientPublicId: String,
    recipientEmail: String,
    template: String,
    data: Map[String, String]
) {
  override def toString: String = s"NewDelivery($recipientPublicId, $template)"
}
