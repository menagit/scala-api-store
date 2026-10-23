package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.catalog.application.{GetManagerProductQuery, GetManagerProductUseCase, ListManagerProductsQuery, ListManagerProductsUseCase}
import com.mendev.apistore.shared.actions.{AuthenticatedAction, RoleAction}
import com.mendev.apistore.shared.security.Manager
import com.mendev.apistore.shared.web.ErrorResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

class ManagerProductController(
  cc: ControllerComponents,
  listManagerProductsUseCase: ListManagerProductsUseCase,
  getManagerProductUseCase: GetManagerProductUseCase,
  authenticated: AuthenticatedAction,
  roleAction: RoleAction
)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def list: Action[AnyContent] = (authenticated andThen roleAction(Manager)).async { request =>
    val query = ListManagerProductsQuery(
      request.getQueryString("page"), request.getQueryString("size"),
      request.getQueryString("sort"), request.getQueryString("order"),
      request.getQueryString("categoryId"), request.getQueryString("status"),
      request.getQueryString("includeDeleted")
    )
    listManagerProductsUseCase.execute(query).map {
      case Right(page) => Ok(ProductJson.managerPageToJson(page))
      case Left(error) => ErrorResponse.result(error)
    }
  }

  def detail(id: Long): Action[AnyContent] = (authenticated andThen roleAction(Manager)).async { request =>
    getManagerProductUseCase.execute(GetManagerProductQuery(id, request.getQueryString("includeDeleted"))).map {
      case Right(item) => Ok(ProductJson.toManagerJson(item))
      case Left(error) => ErrorResponse.result(error)
    }
  }
}
