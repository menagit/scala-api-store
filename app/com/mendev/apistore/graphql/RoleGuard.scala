package com.mendev.apistore.graphql

import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{Actor, Role}

import scala.concurrent.Future

object RoleGuard {

  def require[A](actor: Actor, allowed: Role*)(work: => Future[A]): Future[A] =
    if (allowed.contains(actor.role)) work
    else Future.failed(AppErrorException(AppError.Forbidden()))
}
