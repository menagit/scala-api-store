package com.mendev.apistore.identity.infrastructure.id

import com.mendev.apistore.identity.application.IdGenerator

import java.security.SecureRandom
import java.time.Clock
import java.util.UUID


class UuidV7Generator (clock: Clock) extends IdGenerator {

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