package com.mendev.apistore.notifications.application

final case class OrderStatusChangedEvent(
  orderPublicId: String,
  userPublicId: String,
  to: String,
  trackingNumber: Option[String],
  estimatedDelivery: Option[String]
)
