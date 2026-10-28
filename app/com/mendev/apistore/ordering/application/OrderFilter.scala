package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.OrderStatus

import java.time.Instant
import java.util.UUID

final case class OrderFilter(
  status: Option[OrderStatus],
  from: Option[Instant],
  toExclusive: Option[Instant],
  userPublicId: Option[UUID]
)
