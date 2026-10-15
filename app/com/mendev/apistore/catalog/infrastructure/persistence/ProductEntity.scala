package com.mendev.apistore.catalog.infrastructure.persistence

import com.lucidchart.relate.SqlRow
import com.mendev.apistore.catalog.domain.{Discount, Price, Product, ProductStatus, Stock}
import com.mendev.apistore.catalog.domain.ProductStatus.{Active, Disabled}
import com.mendev.apistore.shared.error.FieldError

import java.time.Instant


final case class ProductEntity(
  id: Long,
  name: String,
  description: String,
  categoryId: Long,
  price: BigDecimal,
  discountPercent: BigDecimal,
  stock: Int,
  status: String,
  deletedAt: Option[Instant],
  createdAt: Instant,
  updatedAt: Instant
)

object ProductEntity {

  def fromRow(row: SqlRow): ProductEntity =
    ProductEntity(
      id = row.long("id"),
      name = row.string("name"),
      description = row.string("description"),
      categoryId = row.long("category_id"),
      price = row.bigDecimal("price"),
      discountPercent = row.bigDecimal("discount_percent"),
      stock = row.int("stock"),
      status = row.string("status"),
      deletedAt = row.instantOption("deleted_at"),
      createdAt = row.instant("created_at"),
      updatedAt = row.instant("updated_at")
    )

  def toDomain(entity: ProductEntity): Product =
    Product(
      id = entity.id,
      name = entity.name,
      description = entity.description,
      categoryId = entity.categoryId,
      price = validOrThrow(Price.from(entity.price), entity.id),
      discount = validOrThrow(Discount.from(entity.discountPercent), entity.id),
      stock = validOrThrow(Stock.from(entity.stock), entity.id),
      status = textToStatus(entity.status, entity.id),
      deletedAt = entity.deletedAt,
      createdAt = entity.createdAt,
      updatedAt = entity.updatedAt
    )


  private[persistence] def statusToText(status: ProductStatus): String = status match {
    case Active   => "ACTIVE"
    case Disabled => "DISABLED"
  }

  private def textToStatus(text: String, id: Long): ProductStatus = text match {
    case "ACTIVE"   => Active
    case "DISABLED" => Disabled
    case other      => throw new IllegalStateException(s"Unknown status '$other' for product $id")
  }

  // Bug or expected failure
  private def validOrThrow[A](result: Either[FieldError, A], id: Long): A =
    result.fold(
      err => throw new IllegalStateException(s"Corrupt ${err.field} for product $id: ${err.message}"),
      identity
    )
}
