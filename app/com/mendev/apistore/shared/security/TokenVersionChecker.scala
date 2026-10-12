package com.mendev.apistore.shared.security

import com.mendev.apistore.shared.error.AppError

import scala.concurrent.Future

trait TokenVersionChecker {
  //Right -> the token is still current.
  //Left(Unauthorized) -> the user is gone or the version is old (the token was revoked).
  // Any other Left -> Some other error.
  def check(tokenClaim: TokenClaims): Future[Either[AppError, Unit]]
}
