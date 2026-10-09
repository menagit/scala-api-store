package com.mendev.apistore.shared.health.web

import com.mendev.apistore.shared.health.HealthCheck
import com.mendev.apistore.shared.web.ErrorResponse
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext

class HealthController (
                                   val controllerComponents: ControllerComponents,
                                   healthCheck: HealthCheck
                                 )(implicit ec: ExecutionContext)
  extends BaseController {

  def health: Action[AnyContent] = Action.async {
    healthCheck.check().map {
      case Right(_) =>
        Ok(Json.obj("status" -> "UP", "database" -> "UP"))
      case Left(error) =>
        ErrorResponse.result(error)
    }
  }
}