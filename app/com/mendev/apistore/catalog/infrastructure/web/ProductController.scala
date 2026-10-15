package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.catalog.application.{CreateProductCommand, CreateProductUseCase, ListProductsQuery, ListProductsUseCase}
import com.mendev.apistore.shared.actions.{AuthenticatedAction, RoleAction}
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.Manager
import com.mendev.apistore.shared.web.ErrorResponse
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ProductController(
  cc: ControllerComponents,
  createProductUseCase: CreateProductUseCase,
  listProductsUseCase: ListProductsUseCase,
  authenticated: AuthenticatedAction,
  roleAction: RoleAction
)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def create: Action[String] = (authenticated andThen roleAction(Manager)).async(parse.tolerantText) { request =>
    readBody[CreateProductRequest](request.body).flatMap(toCommand) match {
      case Left(error) => Future.successful(ErrorResponse.result(error))
      case Right(command) =>
        createProductUseCase.execute(request.actor, command).map {
          case Right(product) => Created(ProductJson.toJson(product))
          case Left(error)    => ErrorResponse.result(error)
        }
    }
  }

  def list: Action[AnyContent] = Action.async { request =>
    val query = ListProductsQuery(
      request.getQueryString("page"), request.getQueryString("size"),
      request.getQueryString("sort"), request.getQueryString("order")
    )
    listProductsUseCase.execute(query).map {
      case Right(page) => Ok(ProductJson.pageToJson(page))
      case Left(error) => ErrorResponse.result(error)
    }
  }

  private val invalidBody: AppError =
    AppError.Validation("Invalid request", List(FieldError("body", "is not valid for this endpoint")))

  private def readBody[A: Reads](body: String): Either[AppError, A] =
    Try(Json.parse(body)).toOption.flatMap(_.asOpt[A]).toRight(invalidBody)

  private def toCommand(req: CreateProductRequest): Either[AppError, CreateProductCommand] =
    (req.name, req.description, req.categoryId, req.price, req.discountPercent, req.stock) match {
      case (Some(name), Some(description), Some(categoryId), Some(price), Some(discountPercent), Some(stock)) =>
        Right(CreateProductCommand(name, description, categoryId, price, discountPercent, stock, req.status))
      case _ =>
        val missing = List(
          "name" -> req.name, "description" -> req.description, "categoryId" -> req.categoryId,
          "price" -> req.price, "discountPercent" -> req.discountPercent, "stock" -> req.stock
        ).collect { case (field, None) => FieldError(field, "is required") }
        Left(AppError.Validation("Invalid request", missing))
    }
}
