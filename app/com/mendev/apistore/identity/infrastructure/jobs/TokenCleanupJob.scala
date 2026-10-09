package com.mendev.apistore.identity.infrastructure.jobs

import com.mendev.apistore.identity.application.CleanupExpiredTokensUseCase
import org.apache.pekko.actor.{ActorSystem, Cancellable}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TokenCleanupJob(
                       cleanup: CleanupExpiredTokensUseCase,
                       actorSystem: ActorSystem,
                       lifecycle: ApplicationLifecycle
                     )(implicit ec: ExecutionContext) {
  import TokenCleanupJob.*

  private val logger = Logger(getClass)

  // Starts when the object is created. Keep the handle so we can cancel it.
  private val task: Cancellable =
    actorSystem.scheduler.scheduleWithFixedDelay(InitialDelay, Interval)(() => runOnce())

  // Stops the timer when the app stops (also on a dev-mode reload).
  lifecycle.addStopHook(() => Future.successful(task.cancel()))

  // Starts one run and returns at once. The result is only logged, never token values.
  private def runOnce(): Unit =
    cleanup.execute().onComplete {
      case Success(Right(count)) => logger.info(s"Token cleanup: $count expired tokens deleted")
      case Success(Left(error))  => logger.warn(s"Token cleanup failed: ${error.message}")
      case Failure(exception)    => logger.error("Token cleanup crashed", exception)
    }
}

object TokenCleanupJob {
  val InitialDelay: FiniteDuration = 1.minute
  val Interval: FiniteDuration     = 1.hour
/*  val InitialDelay: FiniteDuration = 10.second
  val Interval: FiniteDuration     = 1.minute*/
}