package com.mendev.apistore.catalog.domain

import com.mendev.apistore.shared.events.DomainEvent

import java.time.Instant

final case class ProductDiscountChanged(
  productId: Long,
  name: String,
  price: BigDecimal,
  oldDiscountPercent: BigDecimal,
  newDiscountPercent: BigDecimal,
  discountedPrice: BigDecimal,
  at: Instant
) extends DomainEvent {

  override def eventType: String = "ProductDiscountChanged"

  override def payload: Map[String, String] =
    Map(
      "productId"          -> productId.toString,
      "name"               -> name,
      "price"              -> price.toString,
      "oldDiscountPercent" -> oldDiscountPercent.toString,
      "newDiscountPercent" -> newDiscountPercent.toString,
      "discountedPrice"    -> discountedPrice.toString,
      "at"                 -> at.toString
    )

  override def toString: String = s"ProductDiscountChanged($productId)"
}

object ProductDiscountChanged {
  def of(before: Product, after: Product, at: Instant): ProductDiscountChanged =
    ProductDiscountChanged(after.id, after.name, after.price.value, before.discount.percent, after.discount.percent, after.discountedPrice, at)
}
