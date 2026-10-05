package com.mendev.apistore.identity.infrastructure.web

import com.mendev.apistore.identity.application.{SignUp, SignUpCommand}
import com.mendev.apistore.shared.*
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.web.ErrorResponse
import io.circe.Json
import jakarta.inject.{Inject, Singleton}
import play.api.http.MimeTypes
import play.api.libs.circe.Circe
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthController @Inject() (cc: ControllerComponents, signUpUseCase: SignUp)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Circe {

  def register: Action[String] = Action.async(parse.tolerantText) { request =>
    val parsed: Either[AppError, SignUpCommand] =
      io.circe.parser.decode[SignUpRequest](request.body).left.map(_ => invalidBody).flatMap(toCommand)

    parsed match {
      //The error case
      case Left(error) =>
        Future.successful(ErrorResponse.result(error))
        //If goes well...
      case Right(command) =>
        signUpUseCase.execute(command).map {
          case Right(response) =>
            Created(Json.obj("publicId" -> Json.fromString(response.publicId)).noSpaces).as(MimeTypes.JSON)
          case Left(error) =>
            ErrorResponse.result(error)
        }
    }
  }

  private val invalidBody: AppError =
    AppError.Validation("Invalid request", List(FieldError("body", "is not valid for this endpoint")))

  private def toCommand(req: SignUpRequest): Either[AppError, SignUpCommand] = {
    (req.email, req.password, req.firstName, req.lastName) match {
      case (Some(email), Some(password), Some(firstName), Some(lastName)) =>
        Right(SignUpCommand(email = email, firstName = firstName, lastName = lastName, password = password))
      case _ =>
        val missing = List("email" -> req.email, "password" -> req.password,
          "firstName" -> req.firstName, "lastName" -> req.lastName)
          .collect { case (name, None) => FieldError(name, "is required") }
        Left(AppError.Validation("Invalid request", missing))
    }
  }

}
