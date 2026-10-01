package shared.db

import java.sql.Connection

import scala.concurrent.Future

trait TxRunner {

  def run[E, A](work: Connection => Either[E, A]): Future[Either[E, A]]
}