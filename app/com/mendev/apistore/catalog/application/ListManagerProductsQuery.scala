package com.mendev.apistore.catalog.application

final case class ListManagerProductsQuery(
  page: Option[String],
  size: Option[String],
  sort: Option[String],
  order: Option[String],
  categoryId: Option[String],
  status: Option[String],
  includeDeleted: Option[String]
)
