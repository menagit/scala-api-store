package com.mendev.apistore.identity.infrastructure

import com.mendev.apistore.identity.application.IdGenerator
import jakarta.inject.{Inject, Singleton}

import java.security.SecureRandom
import java.time.Clock
import java.util.UUID

@Singleton
class UuidV7Generator @Inject() (clock: Clock) extends IdGenerator {

  private val random = new SecureRandom()

  override def generatePublicId(): String = {
    val millis = clock.millis()
    val randA = random.nextInt(1 << 12).toLong
    val randB = random.nextLong()

    // first 64 bits: 48-bit timestamp, 4-bit version (7), 12 random bits
    val msb = (millis << 16) | (0x7L << 12) | randA
    // last 64 bits: 2-bit variant (10), 62 random bits
    val lsb = (randB & 0x3fffffffffffffffL) | 0x8000000000000000L

    new UUID(msb, lsb).toString
  }
}