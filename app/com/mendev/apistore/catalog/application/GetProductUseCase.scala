package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.ProductStatus.Active
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import scala.concurrent.Future

class GetProductUseCase(productRepo: ProductRepository, categoryRepo: CategoryRepository, imageRepo: ProductImageRepository, storage: ImageStorage, runner: TxRunner) {

  def execute(query: GetProductQuery): Future[Either[AppError, ProductDetail]] =
    runner.run { conn =>
      productRepo.findById(query.id, conn) match {
        case Left(e) => Left(e)
        case Right(Some(p)) if p.status == Active && !p.isDeleted =>
          categoryRepo.findById(p.categoryId, conn).map {
            case Some(category) => ProductListItem(p, category)
            case None           => throw new IllegalStateException(s"Missing category ${p.categoryId} for product ${p.id}")
          }.flatMap { item =>
            imageRepo.findByProduct(p.id, conn).map(images => ProductDetail(item, images.map(ImageView.from(_, storage))))
          }
        case Right(_) => Left(AppError.NotFound("Product not found"))
      }
    }
}
