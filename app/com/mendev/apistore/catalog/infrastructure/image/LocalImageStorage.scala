package com.mendev.apistore.catalog.infrastructure.image

import com.mendev.apistore.catalog.application.ImageStorage

import java.nio.file.{Files, Path}

class LocalImageStorage(dir: Path, baseUrl: String) extends ImageStorage {

  private val root = dir.toAbsolutePath.normalize

  override def put(key: String, bytes: Array[Byte]): Unit = {
    val path = root.resolve(key)
    Files.createDirectories(path.getParent)
    Files.write(path, bytes)
  }

  override def delete(key: String): Unit =
    Files.deleteIfExists(root.resolve(key))

  override def url(key: String): String = s"$baseUrl/$key"

  def file(key: String): Option[Path] = {
    val path = root.resolve(key).normalize
    if (path.startsWith(root) && Files.isRegularFile(path)) Some(path) else None
  }
}
