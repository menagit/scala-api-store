package com.mendev.apistore.catalog

import com.mendev.apistore.catalog.application.{CategoryRepository, ProductRepository, ProductStatusHistoryRepository}
import com.mendev.apistore.catalog.infrastructure.persistence.{RelateCategoryRepository, RelateProductRepository, RelateProductStatusHistoryRepository}
import com.mendev.apistore.shared.SharedComponents
import play.api.ContextBasedBuiltInComponents

trait CatalogComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>   // Play basics + shared (allowed for every context)

  lazy val productRepository: ProductRepository   = new RelateProductRepository
  lazy val categoryRepository: CategoryRepository = new RelateCategoryRepository
  lazy val productStatusHistoryRepository: ProductStatusHistoryRepository = new RelateProductStatusHistoryRepository
}
