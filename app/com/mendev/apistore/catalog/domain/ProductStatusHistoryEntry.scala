package com.mendev.apistore.catalog.domain

import java.time.Instant
import java.util.UUID

final case class ProductStatusHistoryEntry private (
        productId: Long,
        action: ProductStatusAction,
        oldStatus: Option[ProductStatus],
        newStatus: ProductStatus,
        changedByPublicId: UUID,
        createdAt: Instant
                                                   )

object ProductStatusHistoryEntry {

  def created(productId: Long, status: ProductStatus, changedByPublicId: UUID, now: Instant): ProductStatusHistoryEntry =
    new ProductStatusHistoryEntry(productId, ProductStatusAction.Created, None, status, changedByPublicId, now)

  def statusChanged(
                     productId: Long,
                     from: ProductStatus,
                     to: ProductStatus,
                     changedByPublicId: UUID,
                     now: Instant
                   ): ProductStatusHistoryEntry =
    new ProductStatusHistoryEntry(productId, ProductStatusAction.StatusChanged, Some(from), to, changedByPublicId, now)

  def deleted(productId: Long, lastStatus: ProductStatus, changedByPublicId: UUID, now: Instant): ProductStatusHistoryEntry =
    new ProductStatusHistoryEntry(productId, ProductStatusAction.Deleted, Some(lastStatus), lastStatus, changedByPublicId, now)
}
