package com.mendev.apistore.ordering.domain

final case class OrderItem(
  productId: Long,
  productName: String,
  unitPrice: BigDecimal,
  discountPercent: BigDecimal,
  discountedUnitPrice: BigDecimal,
  quantity: Int,
  lineTotal: BigDecimal
)
