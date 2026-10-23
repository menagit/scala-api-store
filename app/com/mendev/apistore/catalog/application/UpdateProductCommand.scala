package com.mendev.apistore.catalog.application

final case class UpdateProductCommand(
  name: Option[String],
  description: Option[String],
  categoryId: Option[Long],
  price: Option[BigDecimal],
  discountPercent: Option[BigDecimal],
  stock: Option[Int]
)
