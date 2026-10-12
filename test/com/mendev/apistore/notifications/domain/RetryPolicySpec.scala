package com.mendev.apistore.notifications.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Duration, Instant}

class RetryPolicySpec extends AnyWordSpec with Matchers {

  private val now = Instant.parse("2026-01-01T10:00:00Z")

  "RetryPolicy" should {
    "retry after 1 minute when the first try failed" in {
      RetryPolicy.nextAttemptAt(1, now) shouldBe Some(now.plus(Duration.ofMinutes(1)))
    }

    "retry after 5 minutes when the second try failed" in {
      RetryPolicy.nextAttemptAt(2, now) shouldBe Some(now.plus(Duration.ofMinutes(5)))
    }

    "give up when the last allowed try failed" in {
      RetryPolicy.nextAttemptAt(RetryPolicy.MaxAttempts, now) shouldBe None
    }
  }
}