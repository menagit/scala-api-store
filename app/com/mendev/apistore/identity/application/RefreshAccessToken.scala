package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.{Clock, Instant}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class RefreshAccessToken(
                          userRepo: UserRepository,
                          tokenRepo: TokenRepository,
                          refreshGenerator: RefreshTokenGenerator,
                          tokenIssuer: TokenIssuer,
                          runner: TxRunner,
                          clock: Clock,
                          refreshTokenTtl: FiniteDuration
                        )(implicit ec: ExecutionContext) {

  // The transaction returns one of these as a Right, so a "reuse" still commits its revokes.
  private sealed trait Outcome
  private case object Reuse extends Outcome
  private final case class Rotated(user: User, newToken: GeneratedRefreshToken) extends Outcome

  // One message for every failure: the client can't tell unknown, expired and reused apart.
  private val invalid: AppError = AppError.Unauthorized("Invalid or expired refresh token")

  //Same Left and right conventions
  def execute(rawToken: String): Future[Either[AppError, SignInResult]] =
    runner.run[AppError, Outcome](conn => rotate(rawToken, conn)).map {
      case Left(error)  => Left(error)
      case Right(Reuse) => Left(invalid)
      case Right(Rotated(user, newToken)) =>
        Right(
          SignInResult(
            tokenIssuer.issue(user),
            IssuedRefreshToken(newToken.raw, refreshTokenTtl.toSeconds)
          )
        )
    }

  private def rotate(rawToken: String, conn: Connection): Either[AppError, Outcome] = {
    val now  = clock.instant()
    val hash = refreshGenerator.hash(rawToken)

    tokenRepo.findByHash(hash, conn) match {
      case None                                                => Left(invalid)
      case Some(token) if token.tokenType != TokenType.Refresh => Left(invalid)
      case Some(token) if token.revokedAt.isDefined            => reuse(token, now, conn)
      case Some(token) if !token.expiresAt.isAfter(now)        => Left(invalid)
      case Some(token) =>
        userRepo.findByPublicId(token.userPublicId, conn).flatMap {
          case None => Left(invalid)
          case Some(user) =>
            // Atomic: only one request can revoke a given token. The loser is a replay.
            if (tokenRepo.revoke(hash, now, conn)) {
              val newToken = refreshGenerator.generate()
              tokenRepo.insert(
                StoredToken(
                  userPublicId = user.publicId,
                  tokenType = TokenType.Refresh,
                  tokenHash = newToken.hash,
                  expiresAt = now.plusSeconds(refreshTokenTtl.toSeconds),
                  revokedAt = None,
                  createdAt = now
                ),
                conn
              )
              Right(Rotated(user, newToken))
            } else reuse(token, now, conn)
        }
    }
  }

  private def reuse(token: StoredToken, now: Instant, conn: Connection): Either[AppError, Outcome] = {
    tokenRepo.revokeAllForUser(token.userPublicId, now, conn)
    Right(Reuse)
  }
}