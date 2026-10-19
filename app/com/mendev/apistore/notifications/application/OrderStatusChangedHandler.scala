package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class OrderStatusChangedHandler(recipients: RecipientDirectory) extends EventHandler[OrderStatusChangedEvent] {

  override def handle(event: OrderStatusChangedEvent, conn: Connection): Either[AppError, List[NewDelivery]] =
    event.to match {
      case "PENDING" => Right(Nil)
      case _         => recipients.find(event.userPublicId, conn).flatMap(toDeliveries(event, _))
    }

  private def toDeliveries(event: OrderStatusChangedEvent, recipient: Option[Recipient]): Either[AppError, List[NewDelivery]] =
    recipient match {
      case None    => Left(AppError.NotFound("Recipient not found"))
      case Some(r) => Right(List(delivery(event, r)))
    }

  private def delivery(event: OrderStatusChangedEvent, recipient: Recipient): NewDelivery =
    NewDelivery(
      recipientPublicId = event.userPublicId,
      recipientEmail    = recipient.email,
      template          = OrderStatusChangedHandler.Template,
      data = Map(
        "name"    -> recipient.firstName,
        "orderId" -> event.orderPublicId,
        "status"  -> event.to
      ) ++ event.trackingNumber.map("trackingNumber" -> _) ++ event.estimatedDelivery.map("estimatedDelivery" -> _)
    )
}

object OrderStatusChangedHandler {
  val Template = "order-status"
}
