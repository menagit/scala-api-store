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

  private def emailError(email: String): Option[FieldError] = {
    if(!User.isValidEmail(email)){
      Some(FieldError("email", "must be in the text@domain.com or .co format"))
    }
    else None
  }

  private def passwordError(pwd: String):   Option[FieldError] = {
    if(!User.isValidPassword(pwd)){
      Some(FieldError("password", s"must be between ${User.MinPasswordLength} and ${User.MaxPasswordLength} characters"))
    }
    else None
  }

  def execute(command: SignUpCommand): Future[Either[AppError, SignUpResponse]] = {
    val pwdValidation = passwordError(command.password)
    val emailValidation = emailError(command.email)
    val errors = List(pwdValidation,emailValidation).flatten
    if(!errors.isEmpty){
      Future.successful(
        Left(AppError.Validation("Invalid request", errors))
      )
    }
    else {
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
