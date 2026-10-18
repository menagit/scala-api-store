package com.mendev.apistore.ordering.domain

sealed trait OrderStatus

object OrderStatus {
  case object Pending   extends OrderStatus
  case object Confirmed extends OrderStatus
  case object Shipped   extends OrderStatus
  case object Delivered extends OrderStatus
  case object Cancelled extends OrderStatus
}
