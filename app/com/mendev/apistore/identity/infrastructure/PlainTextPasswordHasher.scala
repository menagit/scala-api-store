package com.mendev.apistore.identity.infrastructure

import com.mendev.apistore.identity.application.PasswordHasher

// Just temp.... then it's going to be hashed for rel
class PlainTextPasswordHasher extends PasswordHasher {
  override def hash(password: String): String = password
  override def verify(plainPwd: String, storedHash: String): Boolean = {
    plainPwd == storedHash
  }
}