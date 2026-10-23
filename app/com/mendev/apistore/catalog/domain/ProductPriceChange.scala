package com.mendev.apistore.catalog.domain

import java.time.Instant
import java.util.UUID

final case class ProductPriceChange private (
        productId: Long,
        oldPrice: Price,
        newPrice: Price,
        oldDiscount: Discount,
        newDiscount: Discount,
        changedByPublicId: UUID,
        createdAt: Instant
                                            )

object ProductPriceChange {

  // None when neither the price nor the discount changed.
  def of(before: Product, after: Product, by: UUID, now: Instant): Option[ProductPriceChange] =
    if (before.price == after.price && before.discount == after.discount) None
    else Some(new ProductPriceChange(before.id, before.price, after.price, before.discount, after.discount, by, now))
}
