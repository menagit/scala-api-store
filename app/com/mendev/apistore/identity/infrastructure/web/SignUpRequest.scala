package com.mendev.apistore.identity.infrastructure.web

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class SignUpRequest(
  email: Option[String],
  password: Option[String],
  firstName: Option[String],
  lastName: Option[String]
)

object SignUpRequest {
  implicit val decoder: Decoder[SignUpRequest] = deriveDecoder
}