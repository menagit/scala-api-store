package com.mendev.apistore.catalog.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

trait PendingOrderChecker {
  def hasPendingOrders(productId: Long, conn: Connection): Either[AppError, Boolean]
}
