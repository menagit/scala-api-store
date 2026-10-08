package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.config.Secret

//Hash stored, raw one to the client
final case class GeneratedRefreshToken(raw: Secret, hash: Array[Byte])

trait RefreshTokenGenerator {
  def generate(): GeneratedRefreshToken
  def hash(raw: String): Array[Byte]
}