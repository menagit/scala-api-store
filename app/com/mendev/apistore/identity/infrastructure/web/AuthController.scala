package com.mendev.apistore.identity.infrastructure.web

import com.mendev.apistore.identity.application.{SignIn, SignInCommand, SignUp, SignUpCommand}
import com.mendev.apistore.shared.*
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.web.ErrorResponse
import io.circe.Json
import play.api.http.MimeTypes
import play.api.libs.circe.Circe
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import com.mendev.apistore.shared.actions.RateLimitedAction
import scala.concurrent.{ExecutionContext, Future}
import com.mendev.apistore.identity.application.{IssuedRefreshToken, SignIn, SignInCommand, SignUp, SignUpCommand}
import play.api.mvc.{AbstractController, Action, ControllerComponents, Cookie}
import com.mendev.apistore.identity.application.{IssuedRefreshToken, RefreshAccessToken, SignIn, SignInCommand, SignInResult, SignUp, SignUpCommand}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Cookie, DiscardingCookie, Result}

class AuthController (
                                 cc: ControllerComponents,
                                 signUpUseCase: SignUp,
                                 signInUseCase: SignIn,
                                 rateLimited: RateLimitedAction,
                                 refreshUseCase: RefreshAccessToken,
                               )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with Circe {

  //AbstractController(cc) this calls the parent constructor, like super(cc) in Java. It gives us Action, Ok, BadRequest, and the rest
  //Circe == Jackson
  /**
   * SignUp Endpoint
   * @return
   */

    //just play json library -> replace circe
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

  /**
   * SignIn Endpoint
   * @return
   */
  def login: Action[String] = (Action andThen rateLimited("sign-in")).async(parse.tolerantText) { request =>
  //def login: Action[String] = Action.async(parse.tolerantText) { request =>
    val parsed: Either[AppError, SignInCommand] =
      io.circe.parser.decode[SignInRequest](request.body).left.map(_ => invalidBody).flatMap(toSignInCommand)

    parsed match {
      case Left(error) =>
        Future.successful(ErrorResponse.result(error))
      case Right(command) =>
        signInUseCase.execute(command).map {
          case Right(result) =>
            tokenResponse(result)
            val access = result.accessToken
            Ok(
              Json.obj(
                "access_token" -> Json.fromString(access.value),
                "token_type"   -> Json.fromString("Bearer"),
                "expires_in"   -> Json.fromLong(access.expiresInSeconds)
              ).noSpaces
            ).as(MimeTypes.JSON)
              .withHeaders("Cache-Control" -> "no-store", "Pragma" -> "no-cache")
              .withCookies(refreshCookie(result.refreshToken))
          case Left(error) =>
            ErrorResponse.result(error)
        }
    }
  }

  /**
   * Refresh Endpoint: the browser sends the refresh cookie, no body.
   */
  def refresh: Action[AnyContent] = Action.async { request =>
    request.cookies.get(AuthController.RefreshCookieName) match {
      case None =>
        Future.successful(ErrorResponse.result(AppError.Unauthorized("Invalid or expired refresh token")))
      case Some(cookie) =>
        refreshUseCase.execute(cookie.value).map {
          case Right(result) =>
            tokenResponse(result)
            // A dead token: also tell the browser to drop the cookie.
          case Left(error: AppError.Unauthorized) =>
            ErrorResponse.result(error).discardingCookies(discardRefreshCookie)
            // 429, 503, 500...: the token may still be fine, so keep the cookie.
          case Left(error) =>
            ErrorResponse.result(error)
        }
    }
  }

  private val invalidBody: AppError =
    AppError.Validation("Invalid request", List(FieldError("body", "is not valid for this endpoint")))

  /**
   * Helper method to create the signUp command
   * @param req
   * @return
   */
  private def toCommand(req: SignUpRequest): Either[AppError, SignUpCommand] = {
    (req.email, req.password, req.firstName, req.lastName) match {
      case (Some(email), Some(password), Some(firstName), Some(lastName)) =>
        Right(SignUpCommand(email,firstName, lastName, password))
      case _ =>
        val missing = List("email" -> req.email, "password" -> req.password,
          "firstName" -> req.firstName, "lastName" -> req.lastName)
          .collect { case (name, None) => FieldError(name, "is required") }
        Left(AppError.Validation("Invalid request", missing))
    }
  }

  /**
   * Helper method to create the signIn command
   * @param req
   * @return
   */
  private def toSignInCommand(req: SignInRequest): Either[AppError, SignInCommand] =
    (req.email, req.password) match {
      case (Some(email), Some(password)) =>
        Right(SignInCommand(email,password))
      case _ =>
        val missing = List("email" -> req.email, "password" -> req.password)
          .collect { case (name, None) => FieldError(name, "is required") }
        Left(AppError.Validation("Invalid request", missing))
    }
  private def refreshCookie(token: IssuedRefreshToken): Cookie =
    Cookie(
      name = AuthController.RefreshCookieName,
      value = token.value.value,
      maxAge = Some(token.maxAgeSeconds.toInt),
      path = "/auth",
      secure = true,
      httpOnly = true,
      sameSite = Some(Cookie.SameSite.Strict)
    )

  private def tokenResponse(result: SignInResult): Result = {
    val access = result.accessToken
    Ok(
      Json.obj(
        "access_token" -> Json.fromString(access.value),
        "token_type"   -> Json.fromString("Bearer"),
        "expires_in"   -> Json.fromLong(access.expiresInSeconds)
      ).noSpaces
    ).as(MimeTypes.JSON)
      .withHeaders("Cache-Control" -> "no-store", "Pragma" -> "no-cache")
      .withCookies(refreshCookie(result.refreshToken))
  }

  // Same name and path as the cookie we set, or the browser ignores it.
  private val discardRefreshCookie: DiscardingCookie =
    DiscardingCookie(AuthController.RefreshCookieName, path = "/auth", secure = true)
}

object AuthController {
  val RefreshCookieName = "refresh_token"
}