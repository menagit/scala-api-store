package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.{PasswordResetRequested, User}
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.events.EventPublisher
import com.mendev.apistore.shared.security.TokenCipher

import java.time.Clock
import scala.concurrent.Future
import scala.concurrent.duration.*

class ForgotPasswordUseCase(
                             userRepo: UserRepository,
                             tokenRepo: TokenRepository,
                             tokenGenerator: RefreshTokenGenerator, // also makes reset tokens
                             cipher: TokenCipher,
                             events: EventPublisher,
                             runner: TxRunner,
                             clock: Clock
                           ) {
  import ForgotPasswordUseCase.*

  // Same result whether the email exists or not
  def execute(command: ForgotPasswordCommand): Future[Either[AppError, Unit]] = {
    val email = User.normalizeEmail(command.email)
    val now   = clock.instant()

    runner.run { conn =>
      userRepo.findByEmail(email, conn).flatMap {
        case None => Right(())
        case Some(user) =>
          val generated = tokenGenerator.generate()

          // Only the hash is stored.
          tokenRepo.insert(
            StoredToken(
              userPublicId = user.publicId,
              tokenType = TokenType.PasswordReset,
              tokenHash = generated.hash,
              expiresAt = now.plusSeconds(ResetTokenTtl.toSeconds),
              revokedAt = None,
              createdAt = now
            ),
            conn
          )
          // Raw token travels only inside the event.
          events.publish(
            PasswordResetRequested(
              userPublicId = user.publicId,
              email = user.email,
              firstName = user.firstName,
              requestedAt = now,
              encryptedToken = cipher.encrypt(generated.raw.value)
            ),
            now,
            conn
          )

          Right(())
      }
    }
  }
}

object ForgotPasswordUseCase {
  val ResetTokenTtl: FiniteDuration = 30.minutes
}
