package com.mendev.apistore.notifications.infrastructure.jobs

import com.mendev.apistore.notifications.application.{FanOutEventsUseCase, SendDeliveriesUseCase}
import org.apache.pekko.actor.{ActorSystem, Cancellable}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class OutboxPollerJob(
                       fanOut: FanOutEventsUseCase,
                       send: SendDeliveriesUseCase,
                       actorSystem: ActorSystem,
                       lifecycle: ApplicationLifecycle
                     )(implicit ec: ExecutionContext) {
  import OutboxPollerJob.*

  private val logger  = Logger(getClass)
  private val running = new AtomicBoolean(false)

  private val task: Cancellable =
    actorSystem.scheduler.scheduleWithFixedDelay(InitialDelay, Interval)(() => runOnce())

  lifecycle.addStopHook(() => Future.successful(task.cancel()))

  // One run = fan out events, then send due deliveries.
  // If the last run is still going, this tick does nothing.
  private def runOnce(): Unit =
    if (running.compareAndSet(false, true)) {
      fanOutStep().flatMap(_ => sendStep()).onComplete(_ => running.set(false))
    }

  // Each step logs its own result and never fails, so one step cannot stop the other.
  // Logs only ids, types and counts: never payloads, emails or data.
  private def fanOutStep(): Future[Unit] =
    fanOut.execute().map {
      case Right(summary) =>
        if (summary.handled > 0)
          logger.info(s"Outbox: ${summary.handled} events turned into deliveries")
        if (summary.skipped.nonEmpty)
          logger.info(s"Outbox: ${summary.skipped.size} events skipped (no handler): ${summary.skipped.mkString(", ")}")
        if (summary.failed.nonEmpty)
          logger.error(s"Outbox: ${summary.failed.size} events failed and were set aside: ${summary.failed.mkString(", ")}")
      case Left(error) =>
        logger.warn(s"Outbox fan-out failed: ${error.message}")
    }.recover { case NonFatal(e) => logger.error("Outbox fan-out crashed", e) }

  private def sendStep(): Future[Unit] =
    send.execute().map {
      case Right(summary) =>
        if (summary.sent > 0 || summary.retried > 0)
          logger.info(s"Outbox: ${summary.sent} emails sent, ${summary.retried} will retry")
        if (summary.failed.nonEmpty)
          logger.error(s"Outbox: ${summary.failed.size} deliveries failed for good: ${summary.failed.mkString(", ")}")
      case Left(error) =>
        logger.warn(s"Outbox send failed: ${error.message}")
    }.recover { case NonFatal(e) => logger.error("Outbox send crashed", e) }
}

object OutboxPollerJob {
  val InitialDelay: FiniteDuration = 30.seconds
  val Interval: FiniteDuration     = 30.seconds
}