package com.mendev.apistore.notifications.domain

import java.time.{Duration, Instant}

object RetryPolicy {

  val MaxAttempts = 3

  // Wait before retry 1 and before retry 2. One entry fewer than MaxAttempts.
  private val Delays = Vector(Duration.ofMinutes(1), Duration.ofMinutes(5))

  // attemptsMade counts the attempt that just failed. None means give up.
  def nextAttemptAt(attemptsMade: Int, now: Instant): Option[Instant] =
    if (attemptsMade >= MaxAttempts) None
    else Delays.lift(attemptsMade - 1).map(delay => now.plus(delay))
}