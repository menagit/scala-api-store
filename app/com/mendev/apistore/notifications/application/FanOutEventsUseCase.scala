package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import java.time.Clock
import scala.concurrent.Future

// What one run did.
final case class FanOutSummary(handled: Int, skipped: List[StoredEvent], failed: List[StoredEvent])

class FanOutEventsUseCase(
                           eventRepo: EventRepository,
                           deliveryRepo: DeliveryRepository,
                           dispatcher: EventDispatcher,
                           runner: TxRunner,
                           clock: Clock
                         ) {
  import FanOutEventsUseCase.*

  // One run: at most BatchSize events in one transaction.
  // -> failing event is marked failed and the rest go on
  // -> Only an unexpected exception (a database error, a bug) rolls the whole run back.
  def execute(): Future[Either[AppError, FanOutSummary]] =
    runner.run[AppError, FanOutSummary] { conn =>
      val now    = clock.instant()
      val events = eventRepo.findUnprocessed(BatchSize, conn)

      val outcomes = events.map { event =>
        dispatcher.fanOut(event) match {
          case None =>
            eventRepo.markProcessed(event.id, now, conn)
            Skipped(event)

          case Some(Right(deliveries)) if !hasDuplicates(deliveries) =>
            deliveryRepo.insert(event.id, deliveries, now, conn)
            eventRepo.markProcessed(event.id, now, conn)
            Handled

            // A Left, or two deliveries for the same recipient and template (a handler bug).
          case Some(_) =>
            eventRepo.markFailed(event.id, now, conn)
            Failed(event)
        }
      }

      Right(
        FanOutSummary(
          handled = outcomes.count(_ == Handled),
          skipped = outcomes.collect { case Skipped(event) => event },
          failed  = outcomes.collect { case Failed(event) => event }
        )
      )
    }

  private def hasDuplicates(deliveries: List[NewDelivery]): Boolean =
    deliveries.map(d => (d.recipientPublicId, d.template)).distinct.size != deliveries.size
}

object FanOutEventsUseCase {
  val BatchSize = 25

  private sealed trait Outcome
  private case object Handled                    extends Outcome
  private final case class Skipped(event: StoredEvent) extends Outcome
  private final case class Failed(event: StoredEvent)  extends Outcome
}