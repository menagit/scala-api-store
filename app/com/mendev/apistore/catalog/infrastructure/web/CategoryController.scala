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
      case Right(items) =>
        Ok(Json.obj("items" -> items.map(item =>
          Json.obj("id" -> item.category.id, "name" -> item.category.name, "productCount" -> item.productCount))))
      case Left(error)       => ErrorResponse.result(error)
    }
  }
}
