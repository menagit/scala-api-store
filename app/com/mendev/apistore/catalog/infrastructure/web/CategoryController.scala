package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.catalog.application.ListCategoriesUseCase
import com.mendev.apistore.shared.web.ErrorResponse
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

class CategoryController(cc: ControllerComponents, listCategoriesUseCase: ListCategoriesUseCase)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def list: Action[AnyContent] = Action.async {
    listCategoriesUseCase.execute().map {
      case Right(categories) => Ok(Json.obj("items" -> categories.map(c => Json.obj("id" -> c.id, "name" -> c.name))))
      case Left(error)       => ErrorResponse.result(error)
    }
  }
}
