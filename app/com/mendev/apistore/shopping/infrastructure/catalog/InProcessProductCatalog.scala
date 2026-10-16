package com.mendev.apistore.shopping.infrastructure.catalog

import com.mendev.apistore.catalog.application.{GetProductSummariesUseCase, ProductSummary}
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shopping.application.{CartProduct, ProductCatalog}

import scala.concurrent.{ExecutionContext, Future}

class InProcessProductCatalog(getProductSummariesUseCase: GetProductSummariesUseCase)(implicit ec: ExecutionContext)
    extends ProductCatalog {

  override def findByIds(ids: Seq[Long]): Future[Either[AppError, List[CartProduct]]] =
    getProductSummariesUseCase.execute(ids).map(_.map(_.map(toCartProduct)))

  private def toCartProduct(s: ProductSummary): CartProduct =
    CartProduct(s.id, s.name, s.available, s.stock, s.price, s.discountPercent, s.discountedPrice)
}
