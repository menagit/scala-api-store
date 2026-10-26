package com.mendev.apistore.catalog.application

trait ImageStorage {
  def put(key: String, bytes: Array[Byte]): Unit

  def delete(key: String): Unit

  def url(key: String): String
}
