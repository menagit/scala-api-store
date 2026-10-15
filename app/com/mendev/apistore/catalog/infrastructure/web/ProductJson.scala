package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.catalog.domain.{Product, ProductStatus}
import com.mendev.apistore.catalog.domain.ProductStatus.{Active, Disabled}
import play.api.libs.json.{JsObject, Json}

object ProductJson {

  def toJson(p: Product): JsObject =
    Json.obj(
      "id"              -> p.id,
      "name"            -> p.name,
      "description"     -> p.description,
      "categoryId"      -> p.categoryId,
      "price"           -> p.price.value,
      "discountPercent" -> p.discount.percent,
      "stock"           -> p.stock.value,
      "status"          -> statusToText(p.status),
      "createdAt"       -> p.createdAt,
      "updatedAt"       -> p.updatedAt
    )

  private def statusToText(status: ProductStatus): String = status match {
    case Active   => "ACTIVE"
    case Disabled => "DISABLED"
  }
}
