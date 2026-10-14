package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.{PasswordChanged, User}
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.events.EventPublisher

import java.sql.Connection
import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

class ResetPasswordUseCase(
                            userRepo: UserRepository,
                            tokenRepo: TokenRepository,
                            tokenGenerator: RefreshTokenGenerator, // only for its hash method
                            pwdHasher: PasswordHasher,
                            events: EventPublisher,
                            runner: TxRunner,
                            clock: Clock
                          )(implicit ec: ExecutionContext) {


  private val invalidToken: AppError =
    AppError.Validation("Invalid request", List(FieldError("token", "is invalid or expired")))

  def execute(command: ResetPasswordCommand): Future[Either[AppError, Unit]] =
    if (!User.isValidPassword(command.newPwd))
      Future.successful(
        Left(
          AppError.Validation(
            "Invalid request",
            List(FieldError("newPassword", s"must be between ${User.MinPasswordLength} and ${User.MaxPasswordLength} characters"))
          )
        )
      )
    else {
      //The hash on plays thread pool and the db call in the db-dispatcher pool
      Future(pwdHasher.hash(command.newPwd)).flatMap { newHash =>
        runner.run[AppError, Unit](conn => reset(command.token, newHash, conn))
      }
    }

  private def reset(rawToken: String, newHash: String, conn: Connection): Either[AppError, Unit] = {
    val now  = clock.instant()
    val hash = tokenGenerator.hash(rawToken)

    tokenRepo.findByHash(hash, conn) match {
      case Some(token)
        if token.tokenType == TokenType.PasswordReset &&
          token.revokedAt.isEmpty &&
          token.expiresAt.isAfter(now) =>
        if (tokenRepo.revoke(hash, now, conn)) changePassword(token.userPublicId, newHash, now, conn)
        else Left(invalidToken)
      case _ => Left(invalidToken)
    }
  }

  // A Left here rolls the whole transaction back, so the token is not burned.
  private def changePassword(
    userPublicId: String,
    newHash: String,
    now: Instant,
    conn: Connection
                            ): Either[AppError, Unit] =
    userRepo.findByPublicId(userPublicId, conn).flatMap {
      case None => Left(invalidToken)
      case Some(user) =>
        userRepo.updatePasswordHash(user.publicId, newHash, now, conn).flatMap { _ =>
          userRepo.incrementTokenVersion(user.publicId, now, conn).map { _ =>
            //Signs out the user everywhere
            tokenRepo.revokeAllForUser(user.publicId, TokenType.Refresh, now, conn)
            //Closes any other reset links
            tokenRepo.revokeAllForUser(user.publicId, TokenType.PasswordReset, now, conn)
            events.publish(PasswordChanged(user.publicId, user.email, user.firstName, now), now, conn)
          }
        }
    }
}