package com.mendev.apistore.identity

import com.mendev.apistore.identity.application.{IdGenerator, PasswordHasher, RefreshAccessTokenUseCase, RefreshTokenGenerator, SignInUseCase, SignUpUseCase, TokenIssuer, TokenRepository, UserRepository,SignOutUseCase}
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
import com.mendev.apistore.identity.application.CleanupExpiredTokensUseCase
import com.mendev.apistore.identity.infrastructure.jobs.TokenCleanupJob

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

  lazy val signUpUseCase: SignUpUseCase = new SignUpUseCase(idGenerator, userRepository, clock, passwordHasher, txRunner)(executionContext)
  lazy val signOutUseCase: SignOutUseCase = new SignOutUseCase(userRepository, tokenRepository, refreshTokenGenerator, txRunner, clock)
  lazy val signInUseCase: SignInUseCase = new SignInUseCase(
    userRepository, passwordHasher, tokenIssuer, refreshTokenGenerator, tokenRepository,
    txRunner, clock, appConfig.jwt.refreshTokenTtl
  )(executionContext)

  lazy val cleanupExpiredTokensUseCase: CleanupExpiredTokensUseCase =
    new CleanupExpiredTokensUseCase(tokenRepository, txRunner, clock)

  lazy val tokenCleanupJob: TokenCleanupJob =
    new TokenCleanupJob(cleanupExpiredTokensUseCase, actorSystem, applicationLifecycle)(executionContext)

  lazy val authController: AuthController =
    new AuthController(controllerComponents, signUpUseCase, signInUseCase, rateLimitedAction, refreshAccessTokenUseCase,signOutUseCase)(executionContext)

  lazy val refreshAccessTokenUseCase: RefreshAccessTokenUseCase = new RefreshAccessTokenUseCase(
    userRepository, tokenRepository, refreshTokenGenerator, tokenIssuer,
    txRunner, clock, appConfig.jwt.refreshTokenTtl
  )(executionContext)


}