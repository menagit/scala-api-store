package com.mendev.apistore.catalog.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}

import scala.concurrent.Future

class GetManagerProductUseCase(productRepo: ProductRepository, categoryRepo: CategoryRepository, runner: TxRunner) {

  def execute(query: GetManagerProductQuery): Future[Either[AppError, ProductListItem]] =
    parseIncludeDeleted(query.includeDeleted) match {
      case Left(error)           => Future.successful(Left(AppError.Validation("Invalid request", List(error))))
      case Right(includeDeleted) => load(query.id, includeDeleted)
    }

  private def load(id: Long, includeDeleted: Boolean): Future[Either[AppError, ProductListItem]] =
    runner.run { conn =>
      productRepo.findById(id, conn) match {
        case Left(e) => Left(e)
        case Right(Some(p)) if includeDeleted || !p.isDeleted =>
          categoryRepo.findById(p.categoryId, conn).map {
            case Some(category) => ProductListItem(p, category)
            case None           => throw new IllegalStateException(s"Missing category ${p.categoryId} for product ${p.id}")
          }
        case Right(_) => Left(AppError.NotFound("Product not found"))
      }
    }

  private def parseIncludeDeleted(text: Option[String]): Either[FieldError, Boolean] = text match {
    case None          => Right(false)
    case Some("true")  => Right(true)
    case Some("false") => Right(false)
    case Some(_)       => Left(FieldError("includeDeleted", "must be true or false"))
  }
}
