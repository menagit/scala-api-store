package com.mendev.apistore.catalog.application

// The cover is the first image, so it can never differ from images.headOption.
final case class ProductPageItem(item: ProductListItem, images: List[ImageView]) {
  def cover: Option[ImageView] = images.headOption
}
