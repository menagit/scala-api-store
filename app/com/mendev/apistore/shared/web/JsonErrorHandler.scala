package com.mendev.apistore.shared.web

import com.mendev.apistore.shared.error.AppError
import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

class JsonErrorHandler extends HttpErrorHandler with Logging {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    logger.debug(s"Client error $statusCode on ${request.method} ${request.path}: $message")
    val error: AppError = statusCode match {
      case 401 => AppError.Unauthorized()
      case 403 => AppError.Forbidden()
      case 404 => AppError.NotFound(s"No route for ${request.method} ${request.path}")
      case 429 => AppError.TooManyRequests()
      case _   => AppError.Validation("Bad request")
    }
    Future.successful(ErrorResponse.result(error))
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logger.error(s"Unhandled exception on ${request.method} ${request.path}", exception)
    Future.successful(ErrorResponse.result(AppError.Unexpected()))
  }
}