package com.mendev.apistore.catalog

import com.mendev.apistore.catalog.application.{CategoryRepository, ChangeProductStatusUseCase, CreateProductUseCase, DeleteProductUseCase, GetManagerProductUseCase, GetProductSummariesUseCase, GetProductUseCase, ListCategoriesUseCase, ListManagerProductsUseCase, ListProductsUseCase, PendingOrderChecker, ProductPriceHistoryRepository, ProductRepository, ProductStatusHistoryRepository, ReturnStockUseCase, StockRepository, TakeStockUseCase, UpdateProductUseCase}
import com.mendev.apistore.catalog.infrastructure.persistence.{RelateCategoryRepository, RelateProductPriceHistoryRepository, RelateProductRepository, RelateProductStatusHistoryRepository, RelateStockRepository}
import com.mendev.apistore.catalog.infrastructure.web.{CategoryController, ManagerProductController, ProductController}
import com.mendev.apistore.shared.actions.AuthenticatedAction
import com.mendev.apistore.shared.SharedComponents
import play.api.ContextBasedBuiltInComponents

trait CatalogComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>   // Play basics + shared (allowed for every context)

  lazy val productRepository: ProductRepository   = new RelateProductRepository
  lazy val categoryRepository: CategoryRepository = new RelateCategoryRepository
  lazy val productStatusHistoryRepository: ProductStatusHistoryRepository = new RelateProductStatusHistoryRepository
  lazy val stockRepository: StockRepository = new RelateStockRepository
  lazy val productPriceHistoryRepository: ProductPriceHistoryRepository = new RelateProductPriceHistoryRepository

  def authenticatedAction: AuthenticatedAction
  def pendingOrderChecker: PendingOrderChecker

  lazy val createProductUseCase: CreateProductUseCase =
    new CreateProductUseCase(productRepository, categoryRepository, productStatusHistoryRepository, txRunner, clock)

  lazy val listProductsUseCase: ListProductsUseCase =
    new ListProductsUseCase(productRepository, categoryRepository, txRunner)

  lazy val getProductUseCase: GetProductUseCase =
    new GetProductUseCase(productRepository, categoryRepository, txRunner)

  lazy val getProductSummariesUseCase: GetProductSummariesUseCase =
    new GetProductSummariesUseCase(productRepository, txRunner)

  lazy val takeStockUseCase: TakeStockUseCase = new TakeStockUseCase(stockRepository)
  lazy val returnStockUseCase: ReturnStockUseCase = new ReturnStockUseCase(stockRepository)

  lazy val changeProductStatusUseCase: ChangeProductStatusUseCase =
    new ChangeProductStatusUseCase(productRepository, productStatusHistoryRepository, txRunner, clock)

  lazy val deleteProductUseCase: DeleteProductUseCase =
    new DeleteProductUseCase(productRepository, productStatusHistoryRepository, pendingOrderChecker, txRunner, clock)

  lazy val updateProductUseCase: UpdateProductUseCase =
    new UpdateProductUseCase(productRepository, categoryRepository, productPriceHistoryRepository, eventPublisher, txRunner, clock)

  lazy val listCategoriesUseCase: ListCategoriesUseCase =
    new ListCategoriesUseCase(categoryRepository, txRunner)

  lazy val productController: ProductController =
    new ProductController(controllerComponents, createProductUseCase, listProductsUseCase, getProductUseCase, updateProductUseCase, authenticatedAction, roleAction)(executionContext)

  lazy val categoryController: CategoryController =
    new CategoryController(controllerComponents, listCategoriesUseCase)(executionContext)

  lazy val listManagerProductsUseCase: ListManagerProductsUseCase =
    new ListManagerProductsUseCase(productRepository, categoryRepository, txRunner)

  lazy val getManagerProductUseCase: GetManagerProductUseCase =
    new GetManagerProductUseCase(productRepository, categoryRepository, txRunner)

  lazy val managerProductController: ManagerProductController =
    new ManagerProductController(controllerComponents, listManagerProductsUseCase, getManagerProductUseCase, authenticatedAction, roleAction)(executionContext)
}
