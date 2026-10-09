package com.mendev.apistore.identity.infrastructure.security

import com.mendev.apistore.identity.application.{IssuedToken, TokenIssuer}
import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.config.JwtConfig
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{Actor, Client, Manager, Role, TokenClaims, TokenVerifier}
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson, JwtOptions}
import play.api.libs.json.Json
import scala.util.Try
import java.time.Clock

class JwtTokenService (config: JwtConfig, clock: Clock) extends TokenIssuer with TokenVerifier {
  import JwtTokenService.*

  private val key = config.secret.value

  override def issue(user: User): IssuedToken = {
    val issuedAt   = clock.instant().getEpochSecond
    val ttlSeconds = config.accessTokenTtl.toSeconds
    val claim = JwtClaim(
      content = Json.stringify(
        Json.obj(
          RoleClaim    -> roleToText(user.role),
          VersionClaim -> user.tokenVersion
        )
      ),
      subject = Some(user.publicId),
      issuedAt = Some(issuedAt),
      expiration = Some(issuedAt + ttlSeconds)
    )
    IssuedToken(JwtJson.encode(claim, key, Algorithm), ttlSeconds)
  }

  override def verify(token: String): Either[AppError, TokenClaims] =
    JwtJson
      .decode(token, key, Seq(Algorithm), JwtOptions(expiration = false))
      .toOption
      .filter(isNotExpired)
      .flatMap(toTokenClaims)
      .toRight(AppError.Unauthorized(InvalidTokenMessage))

  private def isNotExpired(claim: JwtClaim): Boolean =
    claim.expiration.exists(exp => exp > clock.instant().getEpochSecond)

  private def toTokenClaims(claim: JwtClaim): Option[TokenClaims] =
    for {
      publicId <- claim.subject
      json     <- Try(Json.parse(claim.content)).toOption
      role     <- (json \ RoleClaim).asOpt[String].flatMap(textToRole)
      version  <- (json \ VersionClaim).asOpt[Int]
    } yield TokenClaims(Actor(publicId, role), version)
}

object JwtTokenService {
  private val Algorithm            = JwtAlgorithm.HS256
  private val RoleClaim            = "role"
  private val VersionClaim         = "tv"
  private val InvalidTokenMessage  = "Invalid or expired token"

  private def roleToText(role: Role): String = role match {
    case Manager => "MANAGER"
    case Client  => "CLIENT"
  }

  private def textToRole(text: String): Option[Role] = text match {
    case "MANAGER" => Some(Manager)
    case "CLIENT"  => Some(Client)
    case _         => None
  }
}