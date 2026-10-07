package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.db.TxRunner
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.security.Client

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

class SignUp (idGenerator: IdGenerator,
                        userRepo: UserRepository,
                        clock: Clock,
                        pwdHasher: PasswordHasher,
                        runner: TxRunner
)(implicit ec: ExecutionContext) {



  def execute(command: SignUpCommand): Future[Either[AppError, SignUpResponse]] = {
    val email = User.normalizeEmail(command.email)
    val firstName = command.firstName.strip()
    val lastName = command.lastName.strip()
    val pwdValidation = passwordError(command.password)
    val emailValidation = emailError(email)
    val errors = List(pwdValidation,emailValidation,
      nameError("firstName",firstName),
      nameError("lastName",lastName)).flatten
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
          email = email,
          firstName = firstName,
          lastName = lastName,
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

  private def emailError(email: String): Option[FieldError] =
    if (User.isValidEmail(email)) None
    else Some(FieldError("email", s"must be in the text@domain.com or .co format, up to ${User.MaxEmailLength} characters"))

  private def passwordError(pwd: String):   Option[FieldError] = {
    if(!User.isValidPassword(pwd)){
      Some(FieldError("password", s"must be between ${User.MinPasswordLength} and ${User.MaxPasswordLength} characters"))
    }
    else None
  }

  private def nameError(field: String, name: String): Option[FieldError] =
    if (User.isValidName(name)) None
    else Some(FieldError(field, s"must not be blank and at most ${User.MaxNameLength} characters"))

}
