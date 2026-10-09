package com.mendev.apistore.identity.infrastructure.web

import com.mendev.apistore.identity.application.{IssuedRefreshToken, RefreshAccessTokenUseCase, SignInUseCase, SignInCommand, SignInResult, SignOutUseCase, SignUpUseCase, SignUpCommand}
import com.mendev.apistore.shared.*
import com.mendev.apistore.shared.error.{AppError, FieldError}
import com.mendev.apistore.shared.web.ErrorResponse
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import com.mendev.apistore.shared.actions.RateLimitedAction
import scala.concurrent.{ExecutionContext, Future}
import com.mendev.apistore.identity.application.{IssuedRefreshToken, SignInUseCase, SignInCommand, SignUpUseCase, SignUpCommand}
import play.api.mvc.{AbstractController, Action, ControllerComponents, Cookie}
import com.mendev.apistore.identity.application.{IssuedRefreshToken, RefreshAccessTokenUseCase, SignInUseCase, SignInCommand, SignInResult, SignUpUseCase, SignUpCommand}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Cookie, DiscardingCookie, Result}
import play.api.libs.json.{Json, Reads}
import scala.util.Try

class AuthController (
                       cc: ControllerComponents,
                       signUpUseCase: SignUpUseCase,
                       signInUseCase: SignInUseCase,
                       rateLimited: RateLimitedAction,
                       refreshUseCase: RefreshAccessTokenUseCase,
                       signOutUseCase: SignOutUseCase,
                               )(implicit ec: ExecutionContext)
  extends AbstractController(cc)  {

  //AbstractController(cc) this calls the parent constructor, like super(cc) in Java. It gives us Action, Ok, BadRequest, and the rest
  //just play json library -> replace circe
  /**
   * SignUp Endpoint
   * @return
   */

    //just play json library -> replace circe
  def register: Action[String] = Action.async(parse.tolerantText) { request =>
    val parsed: Either[AppError, SignUpCommand] =
      //io.circe.parser.decode[SignUpRequest](request.body).left.map(_ => invalidBody).flatMap(toCommand)
      readBody[SignUpRequest](request.body).flatMap(toCommand)
    parsed match {
      //The error case
      case Left(error) =>
        Future.successful(ErrorResponse.result(error))
        //If goes well...
      case Right(command) =>
        signUpUseCase.execute(command).map {
          case Right(response) =>
            //Created(Json.obj("publicId" -> Json.fromString(response.publicId)).noSpaces).as(MimeTypes.JSON)
            Created(Json.obj("publicId" -> response.publicId))
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
    val parsed: Either[AppError, SignInCommand] =
      readBody[SignInRequest](request.body).flatMap(toSignInCommand)

    parsed match {
      case Left(error) =>
        Future.successful(ErrorResponse.result(error))
      case Right(command) =>
        signInUseCase.execute(command).map {
          case Right(result) => tokenResponse(result)
          case Left(error)   => ErrorResponse.result(error)
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

  /**
   * Sign-out Endpoint: the cookie is the proof, no body. Success is 204 and the cookie is cleared.
   */
  def signOut: Action[AnyContent] = Action.async { request =>
    val raw = request.cookies.get(AuthController.RefreshCookieName).map(_.value)
    signOutUseCase.execute(raw).map {
      case Right(_)    => NoContent.discardingCookies(discardRefreshCookie)
        // 503, 500...: cookie kept for the usr to try again
      case Left(error) => ErrorResponse.result(error)
    }
  }

  private val invalidBody: AppError =
    AppError.Validation("Invalid request", List(FieldError("body", "is not valid for this endpoint")))

  private def readBody[A: Reads](body: String): Either[AppError, A] =
    Try(Json.parse(body)).toOption.flatMap(_.asOpt[A]).toRight(invalidBody)
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
        "access_token" -> access.value,
        "token_type"   -> "Bearer",
        "expires_in"   -> access.expiresInSeconds
      )
    )
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