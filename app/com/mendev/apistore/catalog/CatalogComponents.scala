package com.mendev.apistore.catalog

import com.mendev.apistore.catalog.application.{CategoryRepository, ProductRepository}
import com.mendev.apistore.catalog.infrastructure.persistence.{RelateCategoryRepository, RelateProductRepository}
import com.mendev.apistore.shared.SharedComponents
import play.api.ContextBasedBuiltInComponents

trait CatalogComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>   // Play basics + shared (allowed for every context)

  lazy val productRepository: ProductRepository   = new RelateProductRepository
  lazy val categoryRepository: CategoryRepository = new RelateCategoryRepository
}
