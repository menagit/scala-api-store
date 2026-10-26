package com.mendev.apistore.catalog.infrastructure.web

import com.mendev.apistore.catalog.application.{DeleteProductImageUseCase, UploadProductImageCommand, UploadProductImageUseCase}
import com.mendev.apistore.shared.actions.{AuthenticatedAction, RoleAction}
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.Manager
import com.mendev.apistore.shared.web.ErrorResponse
import org.apache.pekko.stream.Materializer
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, MaxSizeExceeded, MultipartFormData, Result}

import scala.concurrent.{ExecutionContext, Future}

class ProductImageController(
  cc: ControllerComponents,
  uploadUseCase: UploadProductImageUseCase,
  deleteUseCase: DeleteProductImageUseCase,
  authenticated: AuthenticatedAction,
  roleAction: RoleAction,
  maxFileBytes: Long
)(implicit ec: ExecutionContext, mat: Materializer)
  extends AbstractController(cc) {

  def upload(id: Long): Action[Either[MaxSizeExceeded, MultipartFormData[TemporaryFile]]] =
    (authenticated andThen roleAction(Manager)).async(parse.maxLength(maxFileBytes, parse.multipartFormData)) { request =>
      request.body match {
        case Left(_) => Future.successful(fileError(s"must be at most ${maxFileBytes / (1024 * 1024)} MB"))
        case Right(form) =>
          form.file("file") match {
            case None => Future.successful(fileError("is required"))
            case Some(part) =>
              uploadUseCase.execute(request.actor, UploadProductImageCommand(id, part.ref.path)).map {
                case Right(image) => Created(ProductJson.imageToJson(image))
                case Left(error)  => ErrorResponse.result(error)
              }
          }
      }
    }

  def delete(id: Long, imageId: Long): Action[AnyContent] = (authenticated andThen roleAction(Manager)).async { request =>
    deleteUseCase.execute(request.actor, id, imageId).map {
      case Right(_)    => NoContent
      case Left(error) => ErrorResponse.result(error)
    }
  }

  private def fileError(message: String): Result =
    ErrorResponse.result(AppError.Validation("Invalid request", List(FieldError("file", message))))
}
