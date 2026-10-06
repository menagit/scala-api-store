package com.mendev.apistore.identity.domain

import com.mendev.apistore.shared.security.Role
import java.time.Instant
import java.util.Locale

case class User(publicId: String, email: String, firstName: String,
                lastName: String, passwordHash: String, role: Role,
                createdAt: Instant, updatedAt: Instant,
                tokenVersion: Int)

object User {

  private val regex = """[^@\s]+@[^@\s]+\.(com|co)""".r

  val MinPasswordLength = 8
  val MaxPasswordLength = 16
  val MaxNameLength = 100
  val MaxEmailLength = 254

  def normalizeEmail(email: String): String =
    email.strip().toLowerCase(Locale.ROOT)

  def isValidName(name: String): Boolean = {
    val stripped = name.strip()
    stripped.nonEmpty && stripped.codePointCount(0, stripped.length) <= MaxNameLength
  }

  def isValidEmail(email: String): Boolean = {
    email.codePointCount(0, email.length) <= MaxEmailLength && regex.matches(email)
  }

  def isValidPassword(password: String): Boolean = {
    val pwdLth = password.codePointCount(0,password.length)
    pwdLth>=MinPasswordLength && pwdLth <= MaxPasswordLength
  }
}