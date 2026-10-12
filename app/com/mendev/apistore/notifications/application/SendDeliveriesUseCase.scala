package com.mendev.apistore.notifications.application

import com.mendev.apistore.notifications.domain.RetryPolicy
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

// What one run did.
// Failed deliveries are given up for good.
final case class SendSummary(sent: Int, retried: Int, failed: List[Delivery])

class SendDeliveriesUseCase(
                             deliveryRepo: DeliveryRepository,
                             renderer: TemplateRenderer,
                             emailSender: EmailSender,
                             runner: TxRunner,
                             clock: Clock
                           )(implicit ec: ExecutionContext) {
  import SendDeliveriesUseCase.*

  // Three steps, so no database connection is held while talking to the mail server
  // 1. short transaction: claim due rows (they become SENDING)
  // 2. no transaction: render and send, one by one
  // 3. short transaction: record the result of each one
  // If step 3 throws, the rows stay SENDING (known gap, a lease comes later).
  def execute(): Future[Either[AppError, SendSummary]] =
    runner
      .run[AppError, List[Delivery]] { conn =>
        Right(deliveryRepo.claimDue(clock.instant(), BatchSize, conn))
      }
      .flatMap {
        case Left(error)    => Future.successful(Left(error))
        case Right(claimed) => sendAll(claimed).flatMap(record)
      }

  // Sequential on purpose: one slow mail server must not open many connections.
  private def sendAll(claimed: List[Delivery]): Future[List[(Delivery, Outcome)]] =
    claimed.foldLeft(Future.successful(List.empty[(Delivery, Outcome)])) { (acc, delivery) =>
      acc.flatMap(done => sendOne(delivery).map(outcome => done :+ ((delivery, outcome))))
    }

  private def sendOne(delivery: Delivery): Future[Outcome] =
    Try(renderer.render(delivery.template, delivery.recipientEmail, delivery.data)).toEither match {
      // A bad template or data will not fix itself, so no retry.
      case Left(_)         => Future.successful(GaveUp(delivery.attempts + 1))
      case Right(Left(_))  => Future.successful(GaveUp(delivery.attempts + 1))
      case Right(Right(message)) =>
        emailSender
          .send(message)
          .map {
            case Right(_) => Sent
            case Left(_)  => retryOrGiveUp(delivery)
          }
          .recover { case NonFatal(_) => retryOrGiveUp(delivery) }
    }

  // attempts counts failed tries, so a first-try success leaves it at 0.
  private def retryOrGiveUp(delivery: Delivery): Outcome = {
    val attemptsMade = delivery.attempts + 1
    RetryPolicy.nextAttemptAt(attemptsMade, clock.instant()) match {
      case Some(at) => Retry(attemptsMade, at)
      case None     => GaveUp(attemptsMade)
    }
  }

  private def record(results: List[(Delivery, Outcome)]): Future[Either[AppError, SendSummary]] =
    runner.run[AppError, SendSummary] { conn =>
      val now = clock.instant()
      results.foreach {
        case (d, Sent)                  => deliveryRepo.markSent(d.id, now, conn)
        case (d, Retry(attempts, at))   => deliveryRepo.markRetry(d.id, attempts, at, now, conn)
        case (d, GaveUp(attempts))      => deliveryRepo.markFailed(d.id, attempts, now, conn)
      }
      Right(
        SendSummary(
          sent    = results.count(_._2 == Sent),
          retried = results.count(_._2.isInstanceOf[Retry]),
          failed  = results.collect { case (d, _: GaveUp) => d }
        )
      )
    }
}

object SendDeliveriesUseCase {
  val BatchSize = 25

  private sealed trait Outcome
  private case object Sent                                       extends Outcome
  private final case class Retry(attempts: Int, at: Instant)     extends Outcome
  private final case class GaveUp(attempts: Int)                 extends Outcome
}