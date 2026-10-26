package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.ImageFormat

final case class ProcessedImage(format: ImageFormat, large: Array[Byte], thumb: Array[Byte])
