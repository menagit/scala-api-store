package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.catalog.domain.ImageFormat
import com.mendev.apistore.catalog.infrastructure.image.LocalImageStorage
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.web.ErrorResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

class ImageFileController(cc: ControllerComponents, storage: LocalImageStorage)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def serve(key: String): Action[AnyContent] = Action {
    (storage.file(key), List(ImageFormat.Jpeg, ImageFormat.Png).find(f => key.endsWith(s".${f.extension}"))) match {
      case (Some(path), Some(format)) => Ok.sendPath(path).as(format.contentType)
      case _                          => ErrorResponse.result(AppError.NotFound("Image not found"))
    }
  }
}
