package com.mendev.apistore.shared.actions

import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Role
import com.mendev.apistore.shared.web.ErrorResponse
import play.api.mvc.{ActionFilter, Result}

import scala.concurrent.{ExecutionContext, Future}

// Runs after AuthenticatedAction: authenticatedAction.andThen(roleAction(Manager)).
// The actor is already on the request. No allowed roles means nobody passes.
class RoleAction(implicit ec: ExecutionContext) {

  def apply(allowed: Role*): ActionFilter[AuthenticatedRequest] = new ActionFilter[AuthenticatedRequest] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: AuthenticatedRequest[A]): Future[Option[Result]] =
      Future.successful(
        if (allowed.contains(request.actor.role)) None
        else Some(ErrorResponse.result(AppError.Forbidden()))
      )
  }
}
