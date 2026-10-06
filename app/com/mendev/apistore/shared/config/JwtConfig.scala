package com.mendev.apistore.shared.config

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.FiniteDuration

final case class JwtConfig(secret: Secret, accessTokenTtl: FiniteDuration) {
  require(
    secret.value.getBytes(StandardCharsets.UTF_8).length >= JwtConfig.MinSecretBytes,
    s"The JWT secret must be at least ${JwtConfig.MinSecretBytes} bytes"
  )
}

object JwtConfig {
  val MinSecretBytes = 32
}