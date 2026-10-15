package com.mendev.apistore.catalog.application

final case class ProductPage(items: List[ProductListItem], page: Int, size: Int, totalItems: Long, totalPages: Long)
