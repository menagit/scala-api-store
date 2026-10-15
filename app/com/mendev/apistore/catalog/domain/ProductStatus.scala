package com.mendev.apistore.catalog.domain

sealed trait ProductStatus

object ProductStatus {
  case object Active   extends ProductStatus
  case object Disabled extends ProductStatus
}