package com.mendev.apistore.shopping.application

import com.mendev.apistore.shared.error.AppError

import scala.concurrent.Future

trait ProductCatalog {
  def findByIds(ids: Seq[Long]): Future[Either[AppError, List[CartProduct]]]
}
