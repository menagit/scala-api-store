package com.mendev.apistore.identity.infrastructure.web

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class SignInRequest(email: Option[String], password: Option[String])

object SignInRequest {
  implicit val decoder: Decoder[SignInRequest] = deriveDecoder
}