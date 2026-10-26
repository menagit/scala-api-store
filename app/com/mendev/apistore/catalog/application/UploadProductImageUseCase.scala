package com.mendev.apistore.catalog.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Actor
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.time.{Clock, Instant}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.util.control.NonFatal

class UploadProductImageUseCase(
  productRepo: ProductRepository,
  imageRepo: ProductImageRepository,
  processor: ImageProcessor,
  storage: ImageStorage,
  runner: TxRunner,
  clock: Clock,
  maxPerProduct: Int,
  imageEc: ExecutionContext
)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(getClass)

  def execute(actor: Actor, command: UploadProductImageCommand): Future[Either[AppError, ImageView]] =
    Future(processor.process(command.file))(imageEc).flatMap {
      case Left(error)  => Future.successful(Left(AppError.Validation("Invalid request", List(error))))
      case Right(image) => store(command.productId, image)
    }

  private def store(productId: Long, image: ProcessedImage): Future[Either[AppError, ImageView]] = {
    val name     = s"products/$productId/${UUID.randomUUID()}"
    val largeKey = s"$name-large.${image.format.extension}"
    val thumbKey = s"$name-thumb.${image.format.extension}"
    val now      = clock.instant()
    Future {
      storage.put(largeKey, image.large)
      storage.put(thumbKey, image.thumb)
    }(imageEc)
      .flatMap(_ => runner.run(conn => save(productId, largeKey, thumbKey, now, conn)))
      .andThen {
        case Success(Right(_)) => ()
        case _                 => List(largeKey, thumbKey).foreach(deleteFile)
      }
  }

  private def save(productId: Long, largeKey: String, thumbKey: String, now: Instant, conn: Connection): Either[AppError, ImageView] =
    productRepo.lockById(productId, conn).flatMap {
      case Some(product) if !product.isDeleted => insert(productId, largeKey, thumbKey, now, conn)
      case _                                   => Left(AppError.NotFound("Product not found"))
    }

  private def insert(productId: Long, largeKey: String, thumbKey: String, now: Instant, conn: Connection): Either[AppError, ImageView] =
    imageRepo.countByProduct(productId, conn).flatMap {
      case count if count >= maxPerProduct => Left(AppError.Conflict(s"Product already has $maxPerProduct images"))
      case _ =>
        imageRepo.nextPosition(productId, conn).flatMap { position =>
          imageRepo
            .insert(productId, position, largeKey, thumbKey, now, conn)
            .map(id => ImageView(id, position, storage.url(largeKey), storage.url(thumbKey)))
        }
    }

  private def deleteFile(key: String): Unit =
    try storage.delete(key)
    catch {
      case NonFatal(e) => logger.warn(s"Could not delete image file $key", e)
    }
}
