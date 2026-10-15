package com.mendev.apistore.catalog.domain

import com.mendev.apistore.shared.error.FieldError

import java.time.Instant

// NewProduct.create is the only way to build one
final case class NewProduct private (
        name: String,
        description: String,
        categoryId: Long,
        price: Price,
        discount: Discount,
        stock: Stock,
        status: ProductStatus,
        createdAt: Instant,
        updatedAt: Instant
                                    )

object NewProduct {
  val MaxNameLength: Int        = 200
  val MaxDescriptionLength: Int = 2000

  // All field errors are reported together.
  def create(
              name: String,
              description: String,
              categoryId: Long,
              price: BigDecimal,
              discountPercent: BigDecimal,
              stock: Int,
              status: ProductStatus,
              now: Instant
            ): Either[List[FieldError], NewProduct] = {
    val cleanName        = name.strip()
    val cleanDescription = description.strip()

    val nameError =
      if (cleanName.isEmpty || cleanName.codePointCount(0, cleanName.length) > MaxNameLength)
        Some(FieldError("name", s"must be between 1 and $MaxNameLength characters"))
      else None
    val descriptionError =
      if (cleanDescription.codePointCount(0, cleanDescription.length) > MaxDescriptionLength)
        Some(FieldError("description", s"must be at most $MaxDescriptionLength characters"))
      else None

    val priceResult    = Price.from(price)
    val discountResult = Discount.from(discountPercent)
    val stockResult    = Stock.from(stock)

    val errors = List(
      nameError,
      descriptionError,
      priceResult.left.toOption,
      discountResult.left.toOption,
      stockResult.left.toOption
    ).flatten

    (priceResult, discountResult, stockResult) match {
      case (Right(p), Right(d), Right(s)) if errors.isEmpty =>
        Right(new NewProduct(cleanName, cleanDescription, categoryId, p, d, s, status, now, now))
      case _ => Left(errors)
    }
  }
}