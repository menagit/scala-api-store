package com.mendev.apistore.identity.infrastructure.security

import com.mendev.apistore.identity.application.{GeneratedRefreshToken, RefreshTokenGenerator}
import com.mendev.apistore.shared.config.Secret
import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, SecureRandom}
import java.util.Base64


class SecureRandomRefreshTokenGenerator extends RefreshTokenGenerator {

  private val random = new SecureRandom()

  override def generate(): GeneratedRefreshToken = {
    val bytes = new Array[Byte](SecureRandomRefreshTokenGenerator.TokenBytes)
    random.nextBytes(bytes)
    val raw = Base64.getUrlEncoder.withoutPadding.encodeToString(bytes)
    GeneratedRefreshToken(Secret(raw), hash(raw))
  }

  override def hash(raw: String): Array[Byte] =
    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))
}

object SecureRandomRefreshTokenGenerator {
  val TokenBytes = 32
}