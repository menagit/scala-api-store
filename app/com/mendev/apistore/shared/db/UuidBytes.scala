package com.mendev.apistore.shared.db

import java.nio.ByteBuffer
import java.util.UUID

object UuidBytes {

  def toBytes(uuid: UUID): Array[Byte] =
    ByteBuffer
      .allocate(16)
      .putLong(uuid.getMostSignificantBits)
      .putLong(uuid.getLeastSignificantBits)
      .array()

  def fromBytes(bytes: Array[Byte]): UUID = {
    val buffer = ByteBuffer.wrap(bytes)
    new UUID(buffer.getLong, buffer.getLong)
  }
}
