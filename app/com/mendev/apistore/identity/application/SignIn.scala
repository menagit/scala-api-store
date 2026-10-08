package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import java.time.Clock
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class SignIn(
              userRepo: UserRepository,
              pwdHasher: PasswordHasher,
              tokenIssuer: TokenIssuer,
              refreshGenerator: RefreshTokenGenerator,
              tokenRepo: TokenRepository,
              runner: TxRunner,
              clock: Clock,
              refreshTokenTtl: FiniteDuration
            )(implicit ec: ExecutionContext) {

  // Checked against when the email is unknown, so the response time is the same.
  private val dummyHash = pwdHasher.hash("dummy-password-for-timing")

  def execute(command: SignInCommand): Future[Either[AppError, SignInResult]] = {
    val email = User.normalizeEmail(command.email)

    runner.run(conn => userRepo.findByEmail(email, conn)).flatMap {
      case Left(error) =>
        Future.successful(Left(error))
      case Right(maybeUser) =>
        Future {
          val storedHash = maybeUser.map(_.passwordHash).getOrElse(dummyHash)
          val matches    = pwdHasher.verify(command.password, storedHash)
          maybeUser.filter(_ => matches)
        }.flatMap {
          case Some(user) => issueTokens(user)
          case None       => Future.successful(Left(AppError.Unauthorized("Invalid email or password")))
        }
    }
  }

  private def issueTokens(user: User): Future[Either[AppError, SignInResult]] = {
    val generated = refreshGenerator.generate()
    val now       = clock.instant()
    val stored = StoredToken(
      userPublicId = user.publicId,
      tokenType = TokenType.Refresh,
      tokenHash = generated.hash,
      expiresAt = now.plusSeconds(refreshTokenTtl.toSeconds),
      revokedAt = None,
      createdAt = now
    )

    runner.run[AppError, Unit] { conn =>
      tokenRepo.insert(stored, conn)
      Right(())
    }.map(_.map { _ =>
      SignInResult(
        tokenIssuer.issue(user),
        IssuedRefreshToken(generated.raw, refreshTokenTtl.toSeconds)
      )
    })
  }
}