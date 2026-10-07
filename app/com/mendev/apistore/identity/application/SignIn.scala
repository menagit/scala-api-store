package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.AppError

import scala.concurrent.{ExecutionContext, Future}


class SignIn (
                         userRepo: UserRepository,
                         pwdHasher: PasswordHasher,
                         tokenIssuer: TokenIssuer,
                         runner: TxRunner
                       )(implicit ec: ExecutionContext) {

  // Checked against when the email is unknown, so the response time is the same.
  private val dummyHash = pwdHasher.hash("dummy-password-for-timing")

  def execute(command: SignInCommand): Future[Either[AppError, IssuedToken]] = {
    val email = User.normalizeEmail(command.email)

    runner.run(conn => userRepo.findByEmail(email, conn)).flatMap {
      case Left(error) =>
        Future.successful(Left(error))
      case Right(maybeUser) =>
        Future {
          val storedHash = maybeUser.map(_.passwordHash).getOrElse(dummyHash)
          val matches    = pwdHasher.verify(command.password, storedHash)
          maybeUser.filter(_ => matches)
        }.map {
          case Some(user) => Right(tokenIssuer.issue(user))
          case None       => Left(AppError.Unauthorized("Invalid email or password"))
        }
    }
  }
}