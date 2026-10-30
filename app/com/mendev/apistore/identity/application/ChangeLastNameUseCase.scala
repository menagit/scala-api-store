package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.Actor
import com.mendev.apistore.identity.domain.User

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

class ChangeLastNameUseCase(userRepo: UserRepository,
                            clock: Clock,
                            runner: TxRunner
                           )(implicit ec: ExecutionContext) {

  def execute(command: UpdateLastNameCommand, actor: Actor): Future[Either[AppError, Unit]] = {
    if(!User.isValidName(command.lastName.strip()))
      Future.successful(
        Left(
          AppError.Validation(
            "Invalid Req",
            List(FieldError("lastName",s"must be < than ${User.MaxNameLength}"))
          )
        )
      )
    else {
      val now = clock.instant()
      runner.run{conn =>
        userRepo.findByPublicId(actor.publicId,conn).flatMap{
          case None => Left(AppError.Unauthorized())
          case Some(_) => userRepo.updateLastName(actor.publicId,command.lastName.strip(),conn,now)
        }


      }
/*      runner.run{ conn =>
        userRepo.findByPublicId(actor.publicId,conn) match {
          case Left(error) => Left(error)
          case Right(None) => Left(AppError.Unauthorized())
          //This would be the Some
          case Right(_) =>
          userRepo.updateLastName(actor.publicId,command.lastName.strip(),conn,now)
        }
      }*/
/*      runner.run(conn => userRepo.findByPublicId(actor.publicId,conn)).flatMap{

      }*/
    }
  }

}
