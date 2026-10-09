package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import java.time.Clock
import scala.concurrent.Future

class CleanupExpiredTokensUseCase(
                                   tokenRepo: TokenRepository,
                                   runner: TxRunner,
                                   clock: Clock
                                 ) {
  import CleanupExpiredTokensUseCase.BatchSize

  // One cleanup run: deletes at most BatchSize expired tokens in one transaction.
  // The rest wait for the next run. No calling itself if there's more than the batch size
  def execute(): Future[Either[AppError, Int]] =
    runner.run[AppError, Int](conn => Right(tokenRepo.deleteExpired(clock.instant(), BatchSize, conn)))
}

object CleanupExpiredTokensUseCase {
  val BatchSize = 100
}