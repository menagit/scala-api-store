package com.mendev.apistore.shared.security

import com.mendev.apistore.shared.config.Secret
import com.mendev.apistore.shared.error.AppError

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
import scala.util.Try

// Encrypts short secrets (like a reset token) before they are stored.
// AES-256-GCM: GCM also detects a changed value, so a wrong key or a damaged text fails to decrypt.
// The stored text is Base64 of: 12-byte random IV, then the encrypted bytes.
final class TokenCipher(key: Secret) {

  private val Transformation = "AES/GCM/NoPadding"
  private val KeyBytes       = 32   // 256 bits
  private val IvBytes        = 12   // the usual size for GCM
  private val TagBits        = 128  // the check that detects changes

  // A bad key stops the app at startup.
  private val keySpec: SecretKeySpec = {
    val bytes = Base64.getDecoder.decode(key.value)
    require(bytes.length == KeyBytes, s"Encryption key must be $KeyBytes bytes (AES-256)")
    new SecretKeySpec(bytes, "AES")
  }

  private val random = new SecureRandom()

  // Throws only on a bug. A new IV every time, so the same input gives a different text.
  def encrypt(plain: String): String = {
    val iv = new Array[Byte](IvBytes)
    random.nextBytes(iv)

    val cipher = Cipher.getInstance(Transformation)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TagBits, iv))
    val encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8))

    Base64.getEncoder.encodeToString(iv ++ encrypted)
  }

  // Wrong key, changed value or bad text: Left. The error never contains the value.
  def decrypt(encrypted: String): Either[AppError, String] =
    Try {
      val (iv, body) = Base64.getDecoder.decode(encrypted).splitAt(IvBytes)

      val cipher = Cipher.getInstance(Transformation)
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TagBits, iv))
      new String(cipher.doFinal(body), StandardCharsets.UTF_8)
    }.toEither.left.map(_ => AppError.Unexpected("Could not decrypt the value"))
}

