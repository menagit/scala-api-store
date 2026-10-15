package com.mendev.apistore.catalog.application

sealed trait ProductSort

object ProductSort {
  case object Price     extends ProductSort
  case object Name      extends ProductSort
  case object CreatedAt extends ProductSort
}

sealed trait SortOrder

object SortOrder {
  case object Asc  extends SortOrder
  case object Desc extends SortOrder
}
