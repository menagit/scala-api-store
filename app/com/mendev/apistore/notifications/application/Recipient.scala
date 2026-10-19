package com.mendev.apistore.notifications.application

final case class Recipient(email: String, firstName: String) {
  override def toString: String = "Recipient(***)"
}
