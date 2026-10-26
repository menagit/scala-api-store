package com.mendev.apistore.catalog.application

import com.mendev.apistore.shared.error.FieldError

import java.nio.file.Path

trait ImageProcessor {
  def process(file: Path): Either[FieldError, ProcessedImage]
}
