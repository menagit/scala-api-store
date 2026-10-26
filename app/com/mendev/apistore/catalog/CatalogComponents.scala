package com.mendev.apistore.catalog

import com.mendev.apistore.catalog.application.{CategoryRepository, ChangeProductStatusUseCase, CreateProductUseCase, DeleteProductImageUseCase, DeleteProductUseCase, GetManagerProductUseCase, GetProductSummariesUseCase, GetProductUseCase, ImageProcessor, ListCategoriesUseCase, ListManagerProductsUseCase, ListProductsUseCase, PendingOrderChecker, ProductImageRepository, ProductPriceHistoryRepository, ProductRepository, ProductStatusHistoryRepository, ReturnStockUseCase, StockRepository, TakeStockUseCase, UpdateProductUseCase, UploadProductImageUseCase}
import com.mendev.apistore.catalog.infrastructure.image.{LocalImageStorage, ThumbnailatorImageProcessor}
import com.mendev.apistore.catalog.infrastructure.persistence.{RelateCategoryRepository, RelateProductImageRepository, RelateProductPriceHistoryRepository, RelateProductRepository, RelateProductStatusHistoryRepository, RelateStockRepository}
import com.mendev.apistore.catalog.infrastructure.web.{CategoryController, ImageFileController, ManagerProductController, ProductController, ProductImageController}
import com.mendev.apistore.shared.actions.AuthenticatedAction
import com.mendev.apistore.shared.SharedComponents
import play.api.ContextBasedBuiltInComponents

import java.nio.file.Paths

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
    new ListProductsUseCase(productRepository, categoryRepository, productImageRepository, imageStorage, txRunner)

  lazy val getProductUseCase: GetProductUseCase =
    new GetProductUseCase(productRepository, categoryRepository, productImageRepository, imageStorage, txRunner)

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
    new ListManagerProductsUseCase(productRepository, categoryRepository, productImageRepository, imageStorage, txRunner)

  lazy val getManagerProductUseCase: GetManagerProductUseCase =
    new GetManagerProductUseCase(productRepository, categoryRepository, productImageRepository, imageStorage, txRunner)

  lazy val managerProductController: ManagerProductController =
    new ManagerProductController(controllerComponents, listManagerProductsUseCase, getManagerProductUseCase, authenticatedAction, roleAction)(executionContext)

  lazy val productImageRepository: ProductImageRepository = new RelateProductImageRepository
  lazy val imageProcessor: ImageProcessor = new ThumbnailatorImageProcessor(appConfig.images.maxDimension)
  lazy val imageStorage: LocalImageStorage = new LocalImageStorage(Paths.get(appConfig.images.storageDir), appConfig.images.baseUrl)

  lazy val uploadProductImageUseCase: UploadProductImageUseCase =
    new UploadProductImageUseCase(
      productRepository, productImageRepository, imageProcessor, imageStorage, txRunner, clock,
      appConfig.images.maxPerProduct, actorSystem.dispatchers.lookup("image-dispatcher")
    )(executionContext)

  lazy val deleteProductImageUseCase: DeleteProductImageUseCase =
    new DeleteProductImageUseCase(productRepository, productImageRepository, imageStorage, txRunner)(executionContext)

  lazy val productImageController: ProductImageController =
    new ProductImageController(
      controllerComponents, uploadProductImageUseCase, deleteProductImageUseCase, authenticatedAction, roleAction,
      appConfig.images.maxFileBytes
    )(executionContext, materializer)

  lazy val imageFileController: ImageFileController =
    new ImageFileController(controllerComponents, imageStorage)(executionContext)
}
