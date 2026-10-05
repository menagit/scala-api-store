package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.Client
import jakarta.inject.Inject
import jakarta.inject.Singleton

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignUp @Inject() (idGenerator: IdGenerator,
                        userRepo: UserRepository,
                        clock: Clock,
                        pwdHasher: PasswordHasher,
                        runner: TxRunner
)(implicit ec: ExecutionContext) {

  def execute(command: SignUpCommand): Future[Either[AppError, SignUpResponse]] = {
    if(!User.isValidEmail(command.email)){
      Future.successful(
        Left(AppError.Validation("Invalid request", List(FieldError("email", "must be in the text@domain.com or .co format"))))
      )
    } else {
      //Not yet needed it's just a string for now...
      val hashed: Future[String] = Future {
        pwdHasher.hash(command.password)
      }
      hashed.flatMap { hash =>
        val now = clock.instant()
        val user = User(
          publicId = idGenerator.generatePublicId(),
          email = command.email,
          firstName = command.firstName,
          lastName = command.lastName,
          passwordHash = hash,
          role = Client,
          createdAt = now,
          updatedAt = now,
          tokenVersion = 0
        )
        runner
          .run(conn => userRepo.save(user, conn))
          .map(result => result.map(_ => SignUpResponse(user.publicId)))
      }
    }
  }
}
