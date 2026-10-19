package com.mendev.apistore.identity.application

final case class UserContact(email: String, firstName: String) {
  override def toString: String = "UserContact(***)"
}
