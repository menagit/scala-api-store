package com.mendev.apistore.identity.infrastructure.security

import com.mendev.apistore.identity.application.PasswordHasher
import com.password4j.types.Argon2
import com.password4j.{Argon2Function, Password}
import jakarta.inject.Singleton

@Singleton
class Argon2PasswordHasher extends PasswordHasher {

  import Argon2PasswordHasher.*

  private val argon2: Argon2Function =
    Argon2Function.getInstance(MemoryKiB, Iterations, Parallelism, OutputLength, Argon2.ID, Version)

  override def hash(password: String): String =
    Password.hash(password).addRandomSalt().`with`(argon2).getResult

  override def verify(plainPwd: String, storedHash: String): Boolean =
    Password.check(plainPwd, storedHash).`with`(argon2)
}

object Argon2PasswordHasher {
  // OWASP minimum row: 7 MiB, 5 passes, 1 lane. To move to config in the refinement.
  private val MemoryKiB = 7168
  private val Iterations = 5
  private val Parallelism = 1
  private val OutputLength = 32
  private val Version = 19
}