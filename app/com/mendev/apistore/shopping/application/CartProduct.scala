package com.mendev.apistore.shopping.application

final case class CartProduct(
  id: Long,
  name: String,
  available: Boolean,
  stock: Int,
  price: BigDecimal,
  discountPercent: BigDecimal,
  discountedPrice: BigDecimal
)
