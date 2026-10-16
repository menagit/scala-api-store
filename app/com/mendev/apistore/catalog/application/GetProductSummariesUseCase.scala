package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.Product
import com.mendev.apistore.catalog.domain.ProductStatus.Active
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import scala.concurrent.Future

class GetProductSummariesUseCase(productRepo: ProductRepository, runner: TxRunner) {

  def execute(ids: Seq[Long]): Future[Either[AppError, List[ProductSummary]]] =
    if (ids.isEmpty) Future.successful(Right(Nil))
    else runner.run(conn => productRepo.findByIds(ids, conn).map(_.map(toSummary)))

  private def toSummary(p: Product): ProductSummary =
    ProductSummary(p.id, p.name, p.status == Active && !p.isDeleted, p.stock.value, p.price.value, p.discount.percent, p.discountedPrice)
}
