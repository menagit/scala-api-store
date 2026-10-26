package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.ProductImage

final case class ImageView(id: Long, position: Int, largeUrl: String, thumbUrl: String)

object ImageView {
  def from(image: ProductImage, storage: ImageStorage): ImageView =
    ImageView(image.id, image.position, storage.url(image.largeKey), storage.url(image.thumbKey))
}
