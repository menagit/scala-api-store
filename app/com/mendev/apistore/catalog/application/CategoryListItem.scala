package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.Category

final case class CategoryListItem(category: Category, productCount: Long)
