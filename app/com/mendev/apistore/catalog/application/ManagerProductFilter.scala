package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.ProductStatus

final case class ManagerProductFilter(categoryId: Option[Long], status: Option[ProductStatus], includeDeleted: Boolean)
