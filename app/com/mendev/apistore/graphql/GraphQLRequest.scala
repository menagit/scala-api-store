package com.mendev.apistore.graphql

import play.api.libs.json.{JsObject, Json, Reads}

final case class GraphQLRequest(query: String, operationName: Option[String], variables: Option[JsObject])

object GraphQLRequest {
  implicit val reads: Reads[GraphQLRequest] = Json.reads[GraphQLRequest]
}
