package com.mendev.apistore.shared.config

import java.util.Base64
import scala.util.Try

final case class EncryptionConfig(key: Secret) {

  // Stops the app if no encryption key
  require(
    EncryptionConfig.isValidKey(key.value),
    s"The encryption key must be ${EncryptionConfig.KeyBytes} bytes, in Base64"
  )
}

object EncryptionConfig {
  val KeyBytes = 32 // AES-256

  // Text that is not Base64 counts as invalid, instead of throwing a different error.
  private def isValidKey(value: String): Boolean =
    Try(Base64.getDecoder.decode(value).length == KeyBytes).getOrElse(false)
}