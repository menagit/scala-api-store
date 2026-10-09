package com.mendev.apistore.identity.infrastructure.web

import play.api.libs.json.{Json, Reads}

final case class SignInRequest(email: Option[String], password: Option[String])

object SignInRequest {
  implicit val reads: Reads[SignInRequest] = Json.reads[SignInRequest]
}