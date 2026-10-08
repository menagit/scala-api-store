package com.mendev.apistore.identity

import com.mendev.apistore.identity.application.{IdGenerator, PasswordHasher, RefreshAccessToken, RefreshTokenGenerator, SignIn, SignUp, TokenIssuer, TokenRepository, UserRepository}
import com.mendev.apistore.identity.infrastructure.id.UuidV7Generator
import com.mendev.apistore.identity.infrastructure.persistence.RelateUserRepository
import com.mendev.apistore.identity.infrastructure.security.{Argon2PasswordHasher, JwtTokenService}
import com.mendev.apistore.identity.infrastructure.web.AuthController
import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shared.security.TokenVerifier
import play.api.ContextBasedBuiltInComponents
import com.mendev.apistore.identity.application.{RefreshTokenGenerator, TokenRepository}
import com.mendev.apistore.identity.infrastructure.persistence.RelateTokenRepository
import com.mendev.apistore.identity.infrastructure.security.SecureRandomRefreshTokenGenerator

trait IdentityComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>   // Play basics + shared (allowed for every context)

  lazy val idGenerator: IdGenerator       = new UuidV7Generator(clock)
  lazy val passwordHasher: PasswordHasher = new Argon2PasswordHasher
  lazy val userRepository: UserRepository = new RelateUserRepository
  lazy val refreshTokenGenerator: RefreshTokenGenerator = new SecureRandomRefreshTokenGenerator
  lazy val tokenRepository: TokenRepository             = new RelateTokenRepository

  // One instance, two roles (Guice had two bindings to the same @Singleton).
  lazy val jwtTokenService: JwtTokenService = new JwtTokenService(appConfig.jwt, clock)
  lazy val tokenIssuer: TokenIssuer         = jwtTokenService
  lazy val tokenVerifier: TokenVerifier     = jwtTokenService

  lazy val signUp: SignUp = new SignUp(idGenerator, userRepository, clock, passwordHasher, txRunner)(executionContext)
  lazy val signIn: SignIn = new SignIn(
    userRepository, passwordHasher, tokenIssuer, refreshTokenGenerator, tokenRepository,
    txRunner, clock, appConfig.jwt.refreshTokenTtl
  )(executionContext)

  lazy val authController: AuthController =
    new AuthController(controllerComponents, signUp, signIn, rateLimitedAction, refreshAccessToken)(executionContext)

  lazy val refreshAccessToken: RefreshAccessToken = new RefreshAccessToken(
    userRepository, tokenRepository, refreshTokenGenerator, tokenIssuer,
    txRunner, clock, appConfig.jwt.refreshTokenTtl
  )(executionContext)


}