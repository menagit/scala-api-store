package com.mendev.apistore.identity.application

final case class UserSummary(publicId: String, email: String, firstName: String, lastName: String) {
  override def toString: String = "UserSummary(***)"
}
