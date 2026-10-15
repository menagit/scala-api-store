package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.SqlRow
import com.mendev.apistore.catalog.domain.{ProductStatus, ProductStatusAction, ProductStatusHistoryEntry}
import com.mendev.apistore.catalog.domain.ProductStatus.{Active, Disabled}
import com.mendev.apistore.catalog.domain.ProductStatusAction.{Created, Deleted, StatusChanged}
import com.mendev.apistore.shared.db.UuidBytes

import java.time.Instant
import java.util.UUID

final case class ProductStatusHistoryEntity(
  productId: Long,
  action: String,
  oldStatus: Option[String],
  newStatus: String,
  changedByPublicId: UUID,
  createdAt: Instant
)

object ProductStatusHistoryEntity {

  def fromDomain(entry: ProductStatusHistoryEntry): ProductStatusHistoryEntity =
    ProductStatusHistoryEntity(
      productId = entry.productId,
      action = actionToText(entry.action),
      oldStatus = entry.oldStatus.map(statusToText),
      newStatus = statusToText(entry.newStatus),
      changedByPublicId = entry.changedByPublicId,
      createdAt = entry.createdAt
    )

  def fromRow(row: SqlRow): ProductStatusHistoryEntity =
    ProductStatusHistoryEntity(
      productId = row.long("product_id"),
      action = row.string("action"),
      oldStatus = row.stringOption("old_status"),
      newStatus = row.string("new_status"),
      changedByPublicId = UuidBytes.fromBytes(row.byteArray("changed_by_public_id")),
      createdAt = row.instant("created_at")
    )

  def toDomain(entity: ProductStatusHistoryEntity): ProductStatusHistoryEntry = {
    val id     = entity.productId
    val by     = entity.changedByPublicId
    val at     = entity.createdAt
    val status = textToStatus(entity.newStatus, id)
    (entity.action, entity.oldStatus) match {
      case ("CREATED", None) =>
        ProductStatusHistoryEntry.created(id, status, by, at)
      case ("STATUS_CHANGED", Some(old)) =>
        ProductStatusHistoryEntry.statusChanged(id, textToStatus(old, id), status, by, at)
      case ("DELETED", Some(old)) if old == entity.newStatus =>
        ProductStatusHistoryEntry.deleted(id, status, by, at)
      case (action, old) =>
        throw new IllegalStateException(s"Corrupt status history for product $id: action '$action', old status $old")
    }
  }

  private def actionToText(action: ProductStatusAction): String = action match {
    case Created       => "CREATED"
    case StatusChanged => "STATUS_CHANGED"
    case Deleted       => "DELETED"
  }

  private def statusToText(status: ProductStatus): String = status match {
    case Active   => "ACTIVE"
    case Disabled => "DISABLED"
  }

  private def textToStatus(text: String, productId: Long): ProductStatus = text match {
    case "ACTIVE"   => Active
    case "DISABLED" => Disabled
    case other      => throw new IllegalStateException(s"Unknown status '$other' for product $productId")
  }
}
