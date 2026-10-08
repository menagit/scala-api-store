package com.mendev.apistore.identity.application

sealed trait TokenType

object TokenType {
  case object Refresh extends TokenType
  case object PasswordReset extends TokenType
}