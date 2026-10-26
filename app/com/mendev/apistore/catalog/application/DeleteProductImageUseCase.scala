package com.mendev.apistore.catalog.application

import com.mendev.apistore.catalog.domain.ProductImage
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Actor
import org.slf4j.LoggerFactory

import java.sql.Connection
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class DeleteProductImageUseCase(
  productRepo: ProductRepository,
  imageRepo: ProductImageRepository,
  storage: ImageStorage,
  runner: TxRunner
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(getClass)

  def execute(actor: Actor, productId: Long, imageId: Long): Future[Either[AppError, Unit]] =
    runner.run(conn => remove(productId, imageId, conn)).map {
      _.map(image => List(image.largeKey, image.thumbKey).foreach(deleteFile))
    }

  private def remove(productId: Long, imageId: Long, conn: Connection): Either[AppError, ProductImage] =
    productRepo.lockById(productId, conn).flatMap {
      case Some(product) if !product.isDeleted =>
        imageRepo.findById(imageId, productId, conn).flatMap {
          case Some(image) => imageRepo.delete(image.id, conn).map(_ => image)
          case None        => Left(AppError.NotFound("Image not found"))
        }
      case _ => Left(AppError.NotFound("Product not found"))
    }

  private def deleteFile(key: String): Unit =
    try storage.delete(key)
    catch {
      case NonFatal(e) => logger.warn(s"Could not delete image file $key", e)
    }
}
