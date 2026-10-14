package com.mendev.apistore.identity.infrastructure.web

import play.api.libs.json.{Json, Reads}

final case class ResetPasswordRequest(token: Option[String], newPassword: Option[String])

object ResetPasswordRequest {
  implicit val reads: Reads[ResetPasswordRequest] = Json.reads[ResetPasswordRequest]
}