package com.mendev.apistore.identity.infrastructure.web

import play.api.libs.json.{Json, Reads}

case class SignUpRequest(
      email: Option[String],
      password: Option[String],
      firstName: Option[String],
      lastName: Option[String]
                        )

object SignUpRequest {
  implicit val reads: Reads[SignUpRequest] = Json.reads[SignUpRequest]
}