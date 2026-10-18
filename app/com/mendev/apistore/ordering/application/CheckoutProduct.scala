package com.mendev.apistore.ordering.application

final case class CheckoutProduct(
  id: Long,
  name: String,
  available: Boolean,
  stock: Int,
  price: BigDecimal,
  discountPercent: BigDecimal,
  discountedPrice: BigDecimal
)
