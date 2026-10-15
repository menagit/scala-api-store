package com.mendev.apistore.catalog.application

final case class ListProductsQuery(page: Option[String], size: Option[String], sort: Option[String], order: Option[String])
