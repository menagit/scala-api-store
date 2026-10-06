package com.mendev.apistore.shared.security

import com.mendev.apistore.shared.error.AppError

trait TokenVerifier {
  def verify(token: String): Either[AppError, TokenClaims]
}
