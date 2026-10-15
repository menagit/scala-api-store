package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.{Category, Product}

final case class ProductListItem(product: Product, category: Category)
