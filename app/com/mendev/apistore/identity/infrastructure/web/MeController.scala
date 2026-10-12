package com.mendev.apistore.identity.infrastructure.web
import com.mendev.apistore.shared.actions.AuthenticatedAction
import com.mendev.apistore.shared.security.{Client, Manager, Role}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.concurrent.ExecutionContext

class MeController(cc: ControllerComponents,authenticated: AuthenticatedAction)
  extends AbstractController(cc){

  def me: Action[AnyContent] = authenticated { request =>
    Ok(
      Json.obj(
        "publicId" -> request.actor.publicId,
        "role"     -> roleToText(request.actor.role)
      )
    )
  }

  private def roleToText(role: Role): String = role match {
    case Manager => "MANAGER"
    case Client  => "CLIENT"
  }

}
