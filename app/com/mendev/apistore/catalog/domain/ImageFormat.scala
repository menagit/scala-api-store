package com.mendev.apistore.catalog.domain

sealed abstract class ImageFormat(val extension: String, val contentType: String)

object ImageFormat {
  case object Jpeg extends ImageFormat("jpg", "image/jpeg")
  case object Png  extends ImageFormat("png", "image/png")
}
