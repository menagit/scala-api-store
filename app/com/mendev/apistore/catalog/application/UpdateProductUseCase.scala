package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.{Discount, Price, Product, ProductDiscountChanged, ProductPriceChange, ProductRules, Stock}
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.events.EventPublisher
import com.mendev.apistore.shared.security.Actor
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.time.{Clock, Instant}
import java.util.UUID
import scala.concurrent.Future

class UpdateProductUseCase(
  productRepo: ProductRepository,
  categoryRepo: CategoryRepository,
  priceHistoryRepo: ProductPriceHistoryRepository,
  eventPublisher: EventPublisher,
  runner: TxRunner,
  clock: Clock
) {

  private val logger = LoggerFactory.getLogger(getClass)

  def execute(actor: Actor, productId: Long, command: UpdateProductCommand): Future[Either[AppError, ProductListItem]] = {
    val by  = UUID.fromString(actor.publicId)
    val now = clock.instant()
    runner.run { conn =>
      productRepo.lockById(productId, conn).flatMap {
        case Some(before) if !before.isDeleted => change(before, command, by, now, conn)
        case _                                 => Left(AppError.NotFound("Product not found"))
      }
    }
  }

  private def change(before: Product, command: UpdateProductCommand, by: UUID, now: Instant, conn: Connection): Either[AppError, ProductListItem] =
    applyCommand(before, command, conn).flatMap {
      case after if after == before => withCategory(before, conn)
      case after =>
        val saved = after.copy(updatedAt = now)
        save(before, saved, by, now, conn).flatMap(_ => withCategory(saved, conn))
    }

  // Absent fields keep the stored value. All field errors are reported together.
  private def applyCommand(before: Product, command: UpdateProductCommand, conn: Connection): Either[AppError, Product] =
    checkCategory(before, command.categoryId, conn).flatMap { categoryId =>
      val name        = command.name.fold[Either[FieldError, String]](Right(before.name))(ProductRules.name)
      val description = command.description.fold[Either[FieldError, String]](Right(before.description))(ProductRules.description)
      val price       = command.price.fold[Either[FieldError, Price]](Right(before.price))(Price.from)
      val discount    = command.discountPercent.fold[Either[FieldError, Discount]](Right(before.discount))(Discount.from)
      val stock       = command.stock.fold[Either[FieldError, Stock]](Right(before.stock))(Stock.from)
      (name, description, categoryId, price, discount, stock) match {
        case (Right(n), Right(desc), Right(c), Right(p), Right(d), Right(s)) =>
          Right(before.copy(name = n, description = desc, categoryId = c, price = p, discount = d, stock = s))
        case _ =>
          val errors = List(name, description, categoryId, price, discount, stock).collect { case Left(e) => e }
          Left(AppError.Validation("Invalid request", errors))
      }
    }

  // Outer Left: database error. Inner Left: the category does not exist.
  private def checkCategory(before: Product, categoryId: Option[Long], conn: Connection): Either[AppError, Either[FieldError, Long]] =
    categoryId match {
      case None     => Right(Right(before.categoryId))
      case Some(id) => categoryRepo.existsById(id, conn).map(exists => if (exists) Right(id) else Left(FieldError("categoryId", "does not exist")))
    }

  private def save(before: Product, after: Product, by: UUID, now: Instant, conn: Connection): Either[AppError, Unit] =
    productRepo
      .update(after, conn)
      .flatMap(_ => recordPriceChange(before, after, by, now, conn))
      .map { _ =>
        if (before.discount != after.discount) eventPublisher.publish(ProductDiscountChanged.of(before, after, now), now, conn)
      }

  private def recordPriceChange(before: Product, after: Product, by: UUID, now: Instant, conn: Connection): Either[AppError, Unit] =
    ProductPriceChange.of(before, after, by, now) match {
      case None => Right(())
      case Some(change) =>
        priceHistoryRepo.insert(change, conn).map { _ =>
          logger.info(
            s"Price changed for product ${change.productId}: price ${change.oldPrice.value} -> ${change.newPrice.value}, " +
              s"discount ${change.oldDiscount.percent} -> ${change.newDiscount.percent}, by ${change.changedByPublicId}"
          )
        }
    }

  private def withCategory(product: Product, conn: Connection): Either[AppError, ProductListItem] =
    categoryRepo.findById(product.categoryId, conn).map {
      case Some(category) => ProductListItem(product, category)
      case None           => throw new IllegalStateException(s"Missing category ${product.categoryId} for product ${product.id}")
    }
}
