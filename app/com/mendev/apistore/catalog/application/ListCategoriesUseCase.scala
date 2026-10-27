package com.mendev.apistore.catalog.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import scala.concurrent.Future

class ListCategoriesUseCase(categoryRepo: CategoryRepository, runner: TxRunner) {

  def execute(): Future[Either[AppError, List[CategoryListItem]]] =
    runner.run(conn => categoryRepo.findAllWithProductCount(conn))
}
