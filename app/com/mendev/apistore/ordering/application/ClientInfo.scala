package com.mendev.apistore.ordering.application

import java.util.UUID

final case class ClientInfo(id: UUID, email: String, firstName: String, lastName: String) {
  override def toString: String = "ClientInfo(***)"
}
