package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Clock
import scala.concurrent.Future

class SignOutUseCase(
               userRepo: UserRepository,
               tokenRepo: TokenRepository,
               refreshGenerator: RefreshTokenGenerator,
               runner: TxRunner,
               clock: Clock
             ) {

  // Always a success: sign-out can be repeated, and a bad cookie gives nothing away.
  def execute(rawToken: Option[String]): Future[Either[AppError, Unit]] =
    rawToken match {
      case None      => Future.successful(Right(()))
      case Some(raw) => runner.run[AppError, Unit](conn => endSession(raw, conn))
    }

  private def endSession(rawToken: String, conn: Connection): Either[AppError, Unit] = {
    val now  = clock.instant()
    val hash = refreshGenerator.hash(rawToken)

    tokenRepo.findByHash(hash, conn) match {
      case Some(token)
        if token.tokenType == TokenType.Refresh &&
          token.revokedAt.isEmpty &&
          token.expiresAt.isAfter(now) =>
        // Atomic revoke: if another request won, there is nothing more to do.
        if (tokenRepo.revoke(hash, now, conn)) userRepo.incrementTokenVersion(token.userPublicId, now, conn)
        else Right(())
      case _ => Right(())
    }
  }
}