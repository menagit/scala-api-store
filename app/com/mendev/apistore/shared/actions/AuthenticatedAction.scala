package com.mendev.apistore.shared.actions

import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{TokenVerifier, TokenVersionChecker}
import com.mendev.apistore.shared.web.ErrorResponse
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedAction(
    bodyParser: BodyParser[AnyContent],
    verifier: TokenVerifier,
    versionChecker: TokenVersionChecker
)(implicit ec: ExecutionContext)
    extends ActionBuilder[AuthenticatedRequest, AnyContent] {
  import AuthenticatedAction.*

  override val parser: BodyParser[AnyContent] = bodyParser
  override protected def executionContext: ExecutionContext = ec

  /** Read bearer token-> check signature and expiry -> check the token version in the database
    * @param request
    * @param block
    * @tparam A
    * @return
    */
  override def invokeBlock[A](
      request: Request[A],
      block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] =
    bearerToken(request) match {
      case None        => Future.successful(ErrorResponse.result(AppError.Unauthorized()))
      case Some(token) =>
        verifier.verify(token) match {
          case Left(error)   => Future.successful(ErrorResponse.result(error))
          case Right(claims) =>
            versionChecker.check(claims).flatMap {
              case Left(error) => Future.successful(ErrorResponse.result(error))
              case Right(_)    => block(new AuthenticatedRequest(claims.actor, request))
            }
        }
    }

  // "Authorization: Bearer <token>". Missing header, other scheme or empty token: None.
  private def bearerToken[A](request: Request[A]): Option[String] =
    request.headers
      .get("Authorization")
      .filter(_.startsWith(BearerPrefix))
      .map(_.substring(BearerPrefix.length).trim)
      .filter(_.nonEmpty)
}

object AuthenticatedAction {
  private val BearerPrefix = "Bearer "
}
