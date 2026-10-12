package com.mendev.apistore.notifications.domain

sealed abstract class DeliveryStatus(val value: String)

object DeliveryStatus {
  case object Pending extends DeliveryStatus("PENDING")
  case object Sending extends DeliveryStatus("SENDING")
  case object Sent extends DeliveryStatus("SENT")
  case object Failed extends DeliveryStatus("FAILED")

  val all: List[DeliveryStatus] = List(Pending, Sending, Sent, Failed)

  def fromValue(value: String): Option[DeliveryStatus] = all.find(_.value == value)
}