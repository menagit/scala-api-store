package com.mendev.apistore.ordering.domain

import java.util.UUID

sealed trait ChangedBy

object ChangedBy {
  final case class Client(publicId: UUID)  extends ChangedBy
  final case class Manager(publicId: UUID) extends ChangedBy
  case object System                       extends ChangedBy
}
