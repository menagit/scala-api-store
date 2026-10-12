package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{TokenClaims, TokenVersionChecker}

import java.sql.Connection
import scala.concurrent.Future

class CheckTokenVersionUseCase(userRepo: UserRepository, runner: TxRunner)
    extends TokenVersionChecker {

  /**
   * Check the user exist on the DB with his publicId
   * @param tokenClaim
   * @return
   */
  override def check(tokenClaim: TokenClaims): Future[Either[AppError, Unit]] = {
    runner.run[AppError, Unit] { conn =>
      userRepo.findByPublicId(tokenClaim.actor.publicId, conn) match {
        case Right(Some(user)) if user.tokenVersion == tokenClaim.tokenVersion => Right(())
        case Right(_)    => Left(AppError.Unauthorized())
        case Left(error) => Left(error)
      }
    }
  }

}
