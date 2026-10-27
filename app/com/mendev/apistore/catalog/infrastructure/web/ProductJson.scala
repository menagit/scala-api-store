package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.catalog.application.{ImageView, ProductDetail, ProductListItem, ProductPage}
import com.mendev.apistore.catalog.domain.{Product, ProductStatus}
import com.mendev.apistore.catalog.domain.ProductStatus.{Active, Disabled}
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}

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

  def imageToJson(image: ImageView): JsObject =
    Json.obj(
      "id"       -> image.id,
      "position" -> image.position,
      "largeUrl" -> image.largeUrl,
      "thumbUrl" -> image.thumbUrl
    )

  def coverToJson(image: ImageView): JsObject =
    Json.obj("largeUrl" -> image.largeUrl, "thumbUrl" -> image.thumbUrl)

  // Public reads list only these fields, so a new field on Product never leaks by accident.
  def toPublicJson(item: ProductListItem): JsObject = {
    val p = item.product
    Json.obj(
      "id"              -> p.id,
      "name"            -> p.name,
      "description"     -> p.description,
      "category"        -> Json.obj("id" -> item.category.id, "name" -> item.category.name),
      "price"           -> p.price.value,
      "discountPercent" -> p.discount.percent,
      "discountedPrice" -> p.discountedPrice,
      "stock"           -> p.stock.value,
      "inStock"         -> p.inStock
    )
  }

  def detailToJson(detail: ProductDetail): JsObject =
    toPublicJson(detail.item) ++ Json.obj("images" -> detail.images.map(imageToJson))

  def pageToJson(page: ProductPage): JsObject =
    Json.obj(
      "items"      -> page.items.map(pageItem => withImages(withCover(toPublicJson(pageItem.item), pageItem.cover), pageItem.images)),
      "page"       -> page.page,
      "size"       -> page.size,
      "totalItems" -> page.totalItems,
      "totalPages" -> page.totalPages
    )

  def toManagerJson(item: ProductListItem): JsObject = {
    val p = item.product
    toPublicJson(item) ++ Json.obj(
      "status"    -> statusToText(p.status),
      "createdAt" -> p.createdAt,
      "updatedAt" -> p.updatedAt
    ) ++ p.deletedAt.fold(Json.obj())(d => Json.obj("deletedAt" -> d))
  }

  def managerDetailToJson(detail: ProductDetail): JsObject =
    toManagerJson(detail.item) ++ Json.obj("images" -> detail.images.map(imageToJson))

  def managerPageToJson(page: ProductPage): JsObject =
    Json.obj(
      "items"      -> page.items.map(pageItem => withImages(withCover(toManagerJson(pageItem.item), pageItem.cover), pageItem.images)),
      "page"       -> page.page,
      "size"       -> page.size,
      "totalItems" -> page.totalItems,
      "totalPages" -> page.totalPages
    )

  private def withCover(json: JsObject, cover: Option[ImageView]): JsObject =
    json ++ Json.obj("coverImage" -> cover.fold[JsValue](JsNull)(coverToJson))

  private def withImages(json: JsObject, images: List[ImageView]): JsObject =
    json ++ Json.obj("images" -> images.map(coverToJson))

  private def statusToText(status: ProductStatus): String = status match {
    case Active   => "ACTIVE"
    case Disabled => "DISABLED"
  }
}
