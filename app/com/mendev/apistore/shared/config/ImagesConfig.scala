package com.mendev.apistore.shared.config

final case class ImagesConfig(
  storageDir: String = "uploads",
  baseUrl: String = "http://localhost:9000/images",
  maxFileBytes: Long = 5242880,
  maxDimension: Int = 6000,
  maxPerProduct: Int = 10
)
