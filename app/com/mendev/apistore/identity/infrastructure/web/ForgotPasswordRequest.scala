package com.mendev.apistore.identity.infrastructure.web

import play.api.libs.json.{Json, Reads}

final case class ForgotPasswordRequest(email: Option[String])

object ForgotPasswordRequest {
  implicit val reads: Reads[ForgotPasswordRequest] = Json.reads[ForgotPasswordRequest]
}