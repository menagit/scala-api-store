package com.mendev.apistore.controllers

import com.mendev.apistore.shared.health.HealthCheck
import com.mendev.apistore.shared.web.ErrorResponse
import io.circe.Json
import jakarta.inject.{Inject, Singleton}
import play.api.http.MimeTypes
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class HealthController @Inject() (
                                   val controllerComponents: ControllerComponents,
                                   healthCheck: HealthCheck
                                 )(implicit ec: ExecutionContext)
  extends BaseController {

  def health: Action[AnyContent] = Action.async {
    healthCheck.check().map {
      case Right(_) =>
        val body = Json.obj("status" -> Json.fromString("UP"), "database" -> Json.fromString("UP"))
        Ok(body.noSpaces).as(MimeTypes.JSON)
      case Left(error) =>
        ErrorResponse.result(error)
    }
  }
}