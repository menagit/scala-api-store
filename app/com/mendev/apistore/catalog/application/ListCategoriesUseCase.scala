package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.Category
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import scala.concurrent.Future

class ListCategoriesUseCase(categoryRepo: CategoryRepository, runner: TxRunner) {

  def execute(): Future[Either[AppError, List[Category]]] =
    runner.run(conn => categoryRepo.findAll(conn).map(_.sortBy(_.name)))
}
