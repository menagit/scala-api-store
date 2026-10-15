package com.mendev.apistore.catalog.domain

import java.time.Instant

// A product read from the database.
final case class Product(
              id: Long,
              name: String,
              description: String,
              categoryId: Long,
              price: Price,
              discount: Discount,
              stock: Stock,
              status: ProductStatus,
              deletedAt: Option[Instant],
              createdAt: Instant,
              updatedAt: Instant
                        ) {

  def isDeleted: Boolean = deletedAt.isDefined

  // Never stored: price x (100 - percent) / 100, half up, 2 decimals (ADR 0002).
  def discountedPrice: BigDecimal =
    (price.value * (100 - discount.percent) / 100).setScale(2, BigDecimal.RoundingMode.HALF_UP)
}