package com.mendev.apistore.ordering.application

final case class ListOrdersQuery(
  page: Int,
  size: Int,
  status: Option[String],
  createdFrom: Option[String],
  createdTo: Option[String],
  clientEmail: Option[String]
)
