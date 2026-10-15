package com.mendev.apistore.catalog.domain

sealed trait ProductStatusAction

object ProductStatusAction {
  case object Created       extends ProductStatusAction
  case object StatusChanged extends ProductStatusAction
  case object Deleted       extends ProductStatusAction
}
