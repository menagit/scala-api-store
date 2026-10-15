package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.{Category, Product}

final case class ProductListItem(product: Product, category: Category)

final case class ProductPage(items: List[ProductListItem], page: Int, size: Int, totalItems: Long, totalPages: Long)
