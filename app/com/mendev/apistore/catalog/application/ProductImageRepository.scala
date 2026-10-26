package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.ProductImage
import com.mendev.apistore.shared.error.AppError

import java.sql.Connection
import java.time.Instant

trait ProductImageRepository {
  def insert(productId: Long, position: Int, largeKey: String, thumbKey: String, now: Instant, conn: Connection): Either[AppError, Long]

  def countByProduct(productId: Long, conn: Connection): Either[AppError, Int]

  def nextPosition(productId: Long, conn: Connection): Either[AppError, Int]

  def findByProduct(productId: Long, conn: Connection): Either[AppError, List[ProductImage]]

  def findCovers(productIds: List[Long], conn: Connection): Either[AppError, Map[Long, ProductImage]]

  def findById(imageId: Long, productId: Long, conn: Connection): Either[AppError, Option[ProductImage]]

  def delete(imageId: Long, conn: Connection): Either[AppError, Unit]
}
