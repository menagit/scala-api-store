package com.mendev.apistore.catalog.application

final case class CreateProductCommand(
  name: String,
  description: String,
  categoryId: Long,
  price: BigDecimal,
  discountPercent: BigDecimal,
  stock: Int,
  status: Option[String]
)
