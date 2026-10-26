package com.mendev.apistore.catalog.infrastructure.image

import com.mendev.apistore.catalog.application.{ImageProcessor, ProcessedImage}
import com.mendev.apistore.catalog.domain.ImageFormat
import com.mendev.apistore.shared.error.FieldError
import net.coobird.thumbnailator.Thumbnails

import java.io.{ByteArrayOutputStream, IOException}
import java.nio.file.{Files, Path}
import javax.imageio.ImageIO
import scala.util.Using

class ThumbnailatorImageProcessor(maxDimension: Int) extends ImageProcessor {

  private val jpegStart = Array(0xFF, 0xD8, 0xFF).map(_.toByte)
  private val pngStart  = Array(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A).map(_.toByte)
  private val invalid   = FieldError("file", "is not a valid image")

  override def process(file: Path): Either[FieldError, ProcessedImage] =
    detectFormat(file) match {
      case None => Left(FieldError("file", "must be a JPEG or PNG image"))
      case Some(format) =>
        readSize(file) match {
          case None => Left(invalid)
          case Some((width, height)) if width > maxDimension || height > maxDimension =>
            Left(FieldError("file", s"must be at most $maxDimension pixels on each side"))
          case Some((width, height)) => resize(file, format, width, height)
        }
    }

  private def detectFormat(file: Path): Option[ImageFormat] = {
    val head = Using.resource(Files.newInputStream(file))(_.readNBytes(8))
    if (head.startsWith(jpegStart)) Some(ImageFormat.Jpeg)
    else if (head.startsWith(pngStart)) Some(ImageFormat.Png)
    else None
  }

  private def readSize(file: Path): Option[(Int, Int)] =
    Using.resource(ImageIO.createImageInputStream(file.toFile)) { input =>
      val readers = ImageIO.getImageReaders(input)
      if (!readers.hasNext) None
      else {
        val reader = readers.next()
        try {
          reader.setInput(input, true, true)
          Some((reader.getWidth(0), reader.getHeight(0)))
        } catch {
          case _: IOException => None
        } finally reader.dispose()
      }
    }

  private def resize(file: Path, format: ImageFormat, width: Int, height: Int): Either[FieldError, ProcessedImage] =
    try Right(ProcessedImage(format, copy(file, format, width, height, 1200), copy(file, format, width, height, 400)))
    catch {
      case _: IOException => Left(invalid)
    }

  private def copy(file: Path, format: ImageFormat, width: Int, height: Int, max: Int): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val builder =
      if (width <= max && height <= max) Thumbnails.of(file.toFile).scale(1.0)
      else Thumbnails.of(file.toFile).size(max, max)
    format match {
      case ImageFormat.Jpeg => builder.outputFormat("jpg").outputQuality(0.8).toOutputStream(out)
      case ImageFormat.Png  => builder.outputFormat("png").toOutputStream(out)
    }
    out.toByteArray
  }
}
