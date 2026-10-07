package com.mendev.apistore.shared.security

import io.github.bucket4j.{Bandwidth, Bucket}
import jakarta.inject.Singleton

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Singleton
class RateLimiter {
  import RateLimiter.*

  // One bucket per scope and key. Idle buckets are never removed yet (refinement list).
  private val buckets = new ConcurrentHashMap[String, Bucket]()

  def tryConsume(scope: String, key: String): Boolean =
    buckets.computeIfAbsent(s"$scope:$key", _ => newBucket()).tryConsume(1)
}

object RateLimiter {
  private val Attempts = 5L
  private val Window   = Duration.ofMinutes(1)

  private def newBucket(): Bucket =
    Bucket
      .builder()
      .addLimit(Bandwidth.builder().capacity(Attempts).refillIntervally(Attempts, Window).build())
      .build()
}