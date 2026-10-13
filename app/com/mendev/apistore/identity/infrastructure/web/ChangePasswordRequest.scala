package com.mendev.apistore.identity.infrastructure.web

import play.api.libs.json.{Json, Reads}

final case class ChangePasswordRequest(currentPassword: Option[String], newPassword: Option[String])

object ChangePasswordRequest {
  implicit val reads: Reads[ChangePasswordRequest] = Json.reads[ChangePasswordRequest]
}