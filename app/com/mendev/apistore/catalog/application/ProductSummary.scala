package com.mendev.apistore.catalog.application

final case class ProductSummary(
  id: Long,
  name: String,
  available: Boolean,
  stock: Int,
  price: BigDecimal,
  discountPercent: BigDecimal,
  discountedPrice: BigDecimal
)
