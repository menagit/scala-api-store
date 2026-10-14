package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.Actor
import com.mendev.apistore.identity.domain.{PasswordChanged, User}
import com.mendev.apistore.shared.events.EventPublisher

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

class ChangePasswordUseCase(userRepo: UserRepository,
   tokenRepo: TokenRepository,
  runner: TxRunner,
  clock: Clock,
  pwdHasher: PasswordHasher,
  events: EventPublisher)
(implicit ec:ExecutionContext){

  def execute(command: ChangePasswordCommand, actor: Actor): Future[Either[AppError, Unit]] = {
    if(!User.isValidPassword(command.newPwd))
      Future.successful(
        Left(
          AppError.Validation(
            "Invalid request",
            List(FieldError("newPassword", s"must be between ${User.MinPasswordLength} and ${User.MaxPasswordLength} characters"))
          )
        )
      )
    else
      runner.run(conn => userRepo.findByPublicId(actor.publicId, conn)).flatMap {
        case Left(error) => Future.successful(Left(error))
        case Right(None) =>
          Future.successful(Left(AppError.Unauthorized()))
        case Right(Some(user)) =>
          Future {
            pwdHasher.verify(command.currentPwd, user.passwordHash)
          }.flatMap {
            case false =>
              Future.successful(
                Left(
                  AppError.Validation(
                    "Invalid request",
                    List(FieldError("currentPassword", "is incorrect"))
                  )
                )
              )
            case true =>
              Future {
                pwdHasher.hash(command.newPwd)
              }.flatMap { newHash =>
                val now = clock.instant()
                runner.run[AppError, Unit] { conn =>
                  userRepo.updatePasswordHash(actor.publicId, newHash, now, conn) match {
                    case Left(error) => Left(error)
                    case Right(_) =>
                      userRepo.incrementTokenVersion(actor.publicId, now, conn) match {
                        case Left(error) => Left(error)
                        case Right(_) =>
                          tokenRepo.revokeAllForUser(actor.publicId, TokenType.Refresh, now, conn)
                          events.publish(PasswordChanged(user.publicId, user.email, user.firstName, now), now, conn)
                          Right(())
                      }
                  }
                  /*for {
                    _ <- userRepo.updatePasswordHash(actor.publicId, newHash, now, conn)
                    _ <- userRepo.incrementTokenVersion(actor.publicId, now, conn)
                  } yield tokenRepo.revokeAllForUser(actor.publicId, now, conn)*/
                }
              }
          }
      }

  }

}
