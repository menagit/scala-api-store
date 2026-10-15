package com.mendev.apistore.catalog.infrastructure.web

import play.api.libs.json.{Json, Reads}

case class CreateProductRequest(
  name: Option[String],
  description: Option[String],
  categoryId: Option[Long],
  price: Option[BigDecimal],
  discountPercent: Option[BigDecimal],
  stock: Option[Int],
  status: Option[String]
)

object CreateProductRequest {
  implicit val reads: Reads[CreateProductRequest] = Json.reads[CreateProductRequest]
}
