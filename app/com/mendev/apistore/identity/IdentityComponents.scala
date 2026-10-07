package com.mendev.apistore.identity

import com.mendev.apistore.identity.application.{IdGenerator, PasswordHasher, SignIn, SignUp, TokenIssuer, UserRepository}
import com.mendev.apistore.identity.infrastructure.id.UuidV7Generator
import com.mendev.apistore.identity.infrastructure.persistence.RelateUserRepository
import com.mendev.apistore.identity.infrastructure.security.{Argon2PasswordHasher, JwtTokenService}
import com.mendev.apistore.identity.infrastructure.web.AuthController
import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shared.security.TokenVerifier
import play.api.ContextBasedBuiltInComponents

trait IdentityComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>   // Play basics + shared (allowed for every context)

  lazy val idGenerator: IdGenerator       = new UuidV7Generator(clock)
  lazy val passwordHasher: PasswordHasher = new Argon2PasswordHasher
  lazy val userRepository: UserRepository = new RelateUserRepository

  // One instance, two roles (Guice had two bindings to the same @Singleton).
  lazy val jwtTokenService: JwtTokenService = new JwtTokenService(appConfig.jwt, clock)
  lazy val tokenIssuer: TokenIssuer         = jwtTokenService
  lazy val tokenVerifier: TokenVerifier     = jwtTokenService

  lazy val signUp: SignUp = new SignUp(idGenerator, userRepository, clock, passwordHasher, txRunner)(executionContext)
  lazy val signIn: SignIn = new SignIn(userRepository, passwordHasher, tokenIssuer, txRunner)(executionContext)

  lazy val authController: AuthController =
    new AuthController(controllerComponents, signUp, signIn, rateLimitedAction)(executionContext)
}