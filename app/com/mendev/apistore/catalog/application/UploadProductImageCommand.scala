package com.mendev.apistore.catalog.application

import java.nio.file.Path

final case class UploadProductImageCommand(productId: Long, file: Path)
