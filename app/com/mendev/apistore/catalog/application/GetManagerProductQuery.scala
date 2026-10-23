package com.mendev.apistore.catalog.application

final case class GetManagerProductQuery(id: Long, includeDeleted: Option[String])
