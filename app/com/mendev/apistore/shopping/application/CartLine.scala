package com.mendev.apistore.shopping.application

final case class CartLine(
  productId: Long,
  name: String,
  quantity: Int,
  price: BigDecimal,
  discountPercent: BigDecimal,
  discountedPrice: BigDecimal,
  lineTotal: BigDecimal,
  available: Boolean
)
