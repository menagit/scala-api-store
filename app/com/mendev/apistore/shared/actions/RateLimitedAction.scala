package com.mendev.apistore.shared.actions

import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.RateLimiter
import com.mendev.apistore.shared.web.ErrorResponse
import play.api.mvc.{ActionFilter, Request, Result}

import scala.concurrent.{ExecutionContext, Future}


class RateLimitedAction (limiter: RateLimiter)(implicit ec: ExecutionContext) {

  def apply(scope: String): ActionFilter[Request] = new ActionFilter[Request] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: Request[A]): Future[Option[Result]] =
      Future.successful(
        if (limiter.tryConsume(scope, request.remoteAddress)) None
        else Some(ErrorResponse.result(AppError.TooManyRequests("Too many attempts, try again later")))
      )
  }
}