package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.OrderItem

import java.time.Instant
import java.util.UUID

final case class NewOrder(
  publicId: UUID,
  userPublicId: UUID,
  total: BigDecimal,
  items: List[OrderItem],
  createdAt: Instant
)
