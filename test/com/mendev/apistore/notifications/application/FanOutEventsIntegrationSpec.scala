package com.mendev.apistore.notifications.application

import com.mendev.apistore.notifications.infrastructure.persistence.{RelateDeliveryRepository, RelateEventRepository}
import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.AppError
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Await
import scala.concurrent.duration.*

class FanOutEventsIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)

  private val oneDelivery =
    NewDelivery("0192f1c8-7b3e-7a10-8c44-5d2f6e9a1b07", "ana@example.test", "welcome", Map("name" -> "Ana", "link" -> "http://example.test"))

  // The event type decides what the fake handler does.
  private val dispatcher = new EventDispatcher {
    override def fanOut(event: StoredEvent): Option[Either[AppError, List[NewDelivery]]] =
      event.eventType match {
        case "GOOD" => Some(Right(List(oneDelivery)))
        case "BAD"  => Some(Left(AppError.Validation("bad payload")))
        case "DUP"  => Some(Right(List(oneDelivery, oneDelivery)))
        case _      => None
      }
  }

  private val useCase = new FanOutEventsUseCase(
    new RelateEventRepository,
    new RelateDeliveryRepository,
    dispatcher,
    TestDatabase.txRunner,
    clock
  )

  override def beforeEach(): Unit = {
    TestDatabase.update("DELETE FROM notifications_delivery")
    TestDatabase.update("DELETE FROM shared_event")
  }

  private def insertEvent(eventType: String): Unit =
    TestDatabase.update(
      s"INSERT INTO shared_event (event_type, payload, created_at) VALUES ('$eventType', '{}', '2026-01-01 09:00:00.000000')"
    )

  private def run() = Await.result(useCase.execute(), 10.seconds)

  // (processed, failed) for the one event of this type
  private def flags(eventType: String): (Boolean, Boolean) =
    TestDatabase
      .query(s"SELECT processed_at IS NOT NULL, failed_at IS NOT NULL FROM shared_event WHERE event_type = '$eventType'")(
        rs => (rs.getBoolean(1), rs.getBoolean(2))
      )
      .head

  private def deliveryStatuses(): List[String] =
    TestDatabase.query("SELECT status FROM notifications_delivery")(_.getString(1))

  "FanOutEventsUseCase" should {

    "turn a handled event into a pending delivery and mark the event processed" in {
      insertEvent("GOOD")

      val summary = run().toOption.value

      summary.handled shouldBe 1
      deliveryStatuses() shouldBe List("PENDING")
      flags("GOOD") shouldBe ((true, false))
    }

    "mark an event with no handler as processed and report it as skipped" in {
      insertEvent("OTHER")

      val summary = run().toOption.value

      summary.skipped.map(_.eventType) shouldBe List("OTHER")
      deliveryStatuses() shouldBe Nil
      flags("OTHER") shouldBe ((true, false))
    }

    "mark an event failed when the handler refuses it" in {
      insertEvent("BAD")

      val summary = run().toOption.value

      summary.failed.map(_.eventType) shouldBe List("BAD")
      flags("BAD") shouldBe ((false, true))
    }

    "mark an event failed, with no rows inserted, when two deliveries repeat recipient and template" in {
      insertEvent("DUP")

      val summary = run().toOption.value

      summary.failed.map(_.eventType) shouldBe List("DUP")
      deliveryStatuses() shouldBe Nil
      flags("DUP") shouldBe ((false, true))
    }

    "keep going after a failed event" in {
      insertEvent("BAD")
      insertEvent("GOOD")

      val summary = run().toOption.value

      summary.handled shouldBe 1
      summary.failed.size shouldBe 1
      deliveryStatuses() shouldBe List("PENDING")
    }

    "not pick up an event it already processed" in {
      insertEvent("GOOD")
      run()

      run().toOption.value.handled shouldBe 0
      deliveryStatuses() shouldBe List("PENDING")
    }
  }

  // Either has no .value in plain ScalaTest: small local helper.
  private implicit class EitherOps[A](either: Either[AppError, A]) {
    def toOption: Option[A] = either.toOption
  }
  private implicit class OptionOps[A](option: Option[A]) {
    def value: A = option.getOrElse(fail("Expected a Right"))
  }
}