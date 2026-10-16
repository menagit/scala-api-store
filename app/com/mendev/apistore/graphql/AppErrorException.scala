package com.mendev.apistore.graphql

import com.mendev.apistore.shared.error.AppError

import scala.concurrent.{ExecutionContext, Future}

final case class AppErrorException(error: AppError) extends Exception

object AppErrorException {
  def orFail[A](result: Future[Either[AppError, A]])(implicit ec: ExecutionContext): Future[A] =
    result.flatMap {
      case Right(value) => Future.successful(value)
      case Left(error)  => Future.failed(AppErrorException(error))
    }
}
