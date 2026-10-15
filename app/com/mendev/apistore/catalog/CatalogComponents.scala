package com.mendev.apistore.catalog

import com.mendev.apistore.catalog.application.{CategoryRepository, CreateProductUseCase, GetProductUseCase, ListProductsUseCase, ProductRepository, ProductStatusHistoryRepository}
import com.mendev.apistore.catalog.infrastructure.persistence.{RelateCategoryRepository, RelateProductRepository, RelateProductStatusHistoryRepository}
import com.mendev.apistore.catalog.infrastructure.web.ProductController
import com.mendev.apistore.shared.actions.AuthenticatedAction
import com.mendev.apistore.shared.SharedComponents
import play.api.ContextBasedBuiltInComponents

trait CatalogComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>   // Play basics + shared (allowed for every context)

  lazy val productRepository: ProductRepository   = new RelateProductRepository
  lazy val categoryRepository: CategoryRepository = new RelateCategoryRepository
  lazy val productStatusHistoryRepository: ProductStatusHistoryRepository = new RelateProductStatusHistoryRepository

  def authenticatedAction: AuthenticatedAction

  lazy val createProductUseCase: CreateProductUseCase =
    new CreateProductUseCase(productRepository, categoryRepository, productStatusHistoryRepository, txRunner, clock)

  lazy val listProductsUseCase: ListProductsUseCase =
    new ListProductsUseCase(productRepository, categoryRepository, txRunner)

  lazy val getProductUseCase: GetProductUseCase =
    new GetProductUseCase(productRepository, categoryRepository, txRunner)

  lazy val productController: ProductController =
    new ProductController(controllerComponents, createProductUseCase, listProductsUseCase, getProductUseCase, authenticatedAction, roleAction)(executionContext)
}
