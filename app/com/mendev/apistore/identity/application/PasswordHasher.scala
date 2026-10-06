package com.mendev.apistore.identity.application

trait PasswordHasher {
  def hash(userPwd: String): String
  def verify(plainPwd: String, storedHash: String): Boolean
}
