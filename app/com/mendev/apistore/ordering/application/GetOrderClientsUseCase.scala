package com.mendev.apistore.ordering.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import java.util.UUID
import scala.concurrent.Future

class GetOrderClientsUseCase(clientDirectory: ClientDirectory, runner: TxRunner) {

  def execute(userPublicIds: List[UUID]): Future[Either[AppError, List[ClientInfo]]] =
    runner.run(conn => clientDirectory.find(userPublicIds, conn))
}
