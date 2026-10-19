package com.mendev.apistore.ordering.application

import com.mendev.apistore.ordering.domain.Order
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.Actor

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

class GetMyOrderUseCase(orderRepo: OrderRepository, runner: TxRunner) {

  def execute(actor: Actor, orderId: String): Future[Either[AppError, Order]] =
    Try(UUID.fromString(orderId)).toOption match {
      case None     => Future.successful(Left(notFound))
      case Some(id) => runner.run(conn => orderRepo.findByPublicId(id, conn).flatMap(ownedBy(actor, _)))
    }

  private def ownedBy(actor: Actor, order: Option[Order]): Either[AppError, Order] = order match {
    case Some(o) if o.userPublicId.toString == actor.publicId => Right(o)
    case _                                                    => Left(notFound)
  }

  private def notFound: AppError = AppError.NotFound("Order not found")
}
