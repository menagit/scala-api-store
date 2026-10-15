package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.{NewProduct, Product, ProductStatus, ProductStatusHistoryEntry}
import com.mendev.apistore.catalog.domain.ProductStatus.{Active, Disabled}
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.Actor

import java.time.Clock
import java.util.UUID
import scala.concurrent.Future

class CreateProductUseCase(
  productRepo: ProductRepository,
  categoryRepo: CategoryRepository,
  historyRepo: ProductStatusHistoryRepository,
  runner: TxRunner,
  clock: Clock
) {

  def execute(actor: Actor, command: CreateProductCommand): Future[Either[AppError, Product]] = {
    val status = parseStatus(command.status)
    val created = NewProduct.create(
      command.name, command.description, command.categoryId, command.price,
      command.discountPercent, command.stock, status.getOrElse(Disabled), clock.instant()
    )
    (created, status) match {
      case (Right(newProduct), Right(_)) => save(actor, newProduct)
      case _ =>
        val errors = created.left.toOption.toList.flatten ++ status.left.toOption
        Future.successful(Left(AppError.Validation("Invalid request", errors)))
    }
  }

  private def save(actor: Actor, p: NewProduct): Future[Either[AppError, Product]] =
    runner.run { conn =>
      for {
        exists <- categoryRepo.existsById(p.categoryId, conn)
        _      <- Either.cond(exists, (), AppError.Validation("Invalid request", List(FieldError("categoryId", "does not exist"))))
        id     <- productRepo.insert(p, conn)
        _      <- historyRepo.insert(ProductStatusHistoryEntry.created(id, p.status, UUID.fromString(actor.publicId), p.createdAt), conn)
      } yield Product(id, p.name, p.description, p.categoryId, p.price, p.discount, p.stock, p.status, None, p.createdAt, p.updatedAt)
    }

  private def parseStatus(text: Option[String]): Either[FieldError, ProductStatus] = text match {
    case None             => Right(Disabled)
    case Some("ACTIVE")   => Right(Active)
    case Some("DISABLED") => Right(Disabled)
    case Some(_)          => Left(FieldError("status", "must be ACTIVE or DISABLED"))
  }
}
