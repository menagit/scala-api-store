package com.mendev.apistore.notifications.application

import com.mendev.apistore.notifications.infrastructure.mail.TwirlTemplateRenderer
import com.mendev.apistore.notifications.infrastructure.persistence.RelateDeliveryRepository
import com.mendev.apistore.shared.db.TestDatabase
import com.mendev.apistore.shared.error.AppError
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class SendDeliveriesIntegrationSpec extends AnyWordSpec with Matchers with EitherValues with BeforeAndAfterEach {

  // "Now" is 10:00 UTC. Rows due at or before this moment are claimed.
  private val clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)

  private val Due    = "2026-01-01 09:59:00.000000"
  private val NotDue = "2026-01-01 10:30:00.000000"

  // Records what it was asked to send and answers with what the test chose.
  private class FakeEmailSender(answer: => Future[Either[AppError, Unit]]) extends EmailSender {
    var sent: List[EmailMessage] = Nil
    override def send(message: EmailMessage): Future[Either[AppError, Unit]] = {
      sent = sent :+ message
      answer
    }
  }

  private def working()    = new FakeEmailSender(Future.successful(Right(())))
  private def rejecting()  = new FakeEmailSender(Future.successful(Left(AppError.Unavailable("mail down"))))
  private def crashing()   = new FakeEmailSender(Future.failed(new RuntimeException("boom")))

  private def useCase(sender: EmailSender) =
    new SendDeliveriesUseCase(
      new RelateDeliveryRepository,
      new TwirlTemplateRenderer,
      sender,
      TestDatabase.txRunner,
      clock
    )

  private def run(sender: EmailSender) = Await.result(useCase(sender).execute(), 10.seconds).value

  override def beforeEach(): Unit = TestDatabase.update("DELETE FROM notifications_delivery")

  // The unique key is (event, recipient, template): each row gets its own event id.
  private var nextEventId = 0L

  private def insertDelivery(
                              template: String = "welcome",
                              attempts: Int = 0,
                              nextAttemptAt: String = Due,
                              data: String = """{"name":"Ana","link":"http://example.test"}"""
                            ): Unit = {
    nextEventId += 1
    TestDatabase.update(
      s"""INSERT INTO notifications_delivery
         |  (event_id, recipient_user_id, recipient_email, template, data, status, attempts,
         |   next_attempt_at, created_at, updated_at)
         |VALUES
         |  ($nextEventId, 1, 'ana@example.test', '$template', '$data', 'PENDING', $attempts,
         |   '$nextAttemptAt', '2026-01-01 09:00:00.000000', '2026-01-01 09:00:00.000000')""".stripMargin
    )
  }

  // (status, attempts, next attempt) of the only row
  private def theRow(): (String, Int, String) =
    TestDatabase
      .query(
        "SELECT status, attempts, DATE_FORMAT(next_attempt_at, '%Y-%m-%d %H:%i:%s') FROM notifications_delivery"
      )(rs => (rs.getString(1), rs.getInt(2), rs.getString(3)))
      .head

  "SendDeliveriesUseCase" should {

    "send a due delivery and mark it SENT" in {
      insertDelivery()
      val sender = working()

      val summary = run(sender)

      summary.sent shouldBe 1
      sender.sent.map(m => (m.to, m.subject)) shouldBe List(("ana@example.test", "Welcome to API Store"))
      theRow()._1 shouldBe "SENT"
      theRow()._2 shouldBe 0
    }

    "schedule a retry when the mail server refuses, and not pick the row again before it is due" in {
      insertDelivery()
      val sender = rejecting()

      val summary = run(sender)

      summary.retried shouldBe 1
      theRow() shouldBe (("PENDING", 1, "2026-01-01 10:01:00"))

      run(sender).retried shouldBe 0
      sender.sent.size shouldBe 1
    }

    "treat a failed Future from the sender like a refused send" in {
      insertDelivery()

      run(crashing()).retried shouldBe 1
      theRow() shouldBe (("PENDING", 1, "2026-01-01 10:01:00"))
    }

    "mark the delivery FAILED after the last allowed try" in {
      insertDelivery(attempts = 2)

      val summary = run(rejecting())

      summary.failed.size shouldBe 1
      theRow()._1 shouldBe "FAILED"
      theRow()._2 shouldBe 3
    }

    "fail at once, without sending, when the template is unknown" in {
      insertDelivery(template = "nope")
      val sender = working()

      run(sender).failed.size shouldBe 1
      sender.sent shouldBe Nil
      theRow()._1 shouldBe "FAILED"
    }

    "fail at once, without sending, when the template data is incomplete" in {
      insertDelivery(data = "{}")
      val sender = working()

      run(sender).failed.size shouldBe 1
      sender.sent shouldBe Nil
      theRow()._1 shouldBe "FAILED"
    }

    "leave a delivery alone until it is due" in {
      insertDelivery(nextAttemptAt = NotDue)
      val sender = working()

      val summary = run(sender)

      summary.sent shouldBe 0
      sender.sent shouldBe Nil
      theRow()._1 shouldBe "PENDING"
    }
  }
}