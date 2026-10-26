package com.mendev.apistore.catalog.domain

import java.time.Instant

final case class ProductImage(id: Long, productId: Long, position: Int, largeKey: String, thumbKey: String, createdAt: Instant)
