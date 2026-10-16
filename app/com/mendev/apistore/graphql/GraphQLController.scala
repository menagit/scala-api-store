package com.mendev.apistore.graphql

import com.mendev.apistore.shared.actions.AuthenticatedAction
import com.mendev.apistore.shared.web.{ErrorResponse, JsonBody}
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class GraphQLController(
  cc: ControllerComponents,
  executor: GraphQLExecutor,
  authenticated: AuthenticatedAction
)(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def execute: Action[String] = authenticated.async(parse.tolerantText) { request =>
    JsonBody.read[GraphQLRequest](request.body) match {
      case Left(error)  => Future.successful(ErrorResponse.result(error))
      case Right(query) => executor.execute(query, GraphQLContext(request.actor)).map(Ok(_))
    }
  }
}
