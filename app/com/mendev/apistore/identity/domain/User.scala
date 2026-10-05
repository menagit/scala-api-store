package com.mendev.apistore.identity.domain

import com.mendev.apistore.shared.security.Role
import java.time.Instant

case class User(publicId: String, email: String, firstName: String,
                lastName: String, passwordHash: String, role: Role,
                createdAt: Instant, updatedAt: Instant,
                tokenVersion: Int)

object User {

  private val regex = """[^@\s]+@[^@\s]+\.(com|co)""".r

  def isValidEmail(email: String): Boolean = {
    regex.matches(email)
  }
}