package com.mendev.apistore.graphql

import com.mendev.apistore.shared.AppComponentsSuite
import com.mendev.apistore.shared.db.TestDatabase
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class GraphQLControllerSpec extends PlaySpec with AppComponentsSuite with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    TestDatabase.update("DELETE FROM identity_token")
    TestDatabase.update("DELETE FROM identity_user")
  }

  private val credentials = Json.obj("email" -> "graphql@apistore.com", "password" -> "Secret1234")

  private def signUpAndSignIn(): (String, String) = {
    val signUp   = FakeRequest(POST, "/auth/sign-up").withJsonBody(credentials ++ Json.obj("firstName" -> "Ana", "lastName" -> "Ruiz"))
    val publicId = (contentAsJson(route(app, signUp).get) \ "publicId").as[String]
    val signIn   = FakeRequest(POST, "/auth/sign-in").withJsonBody(credentials)
    val token    = (contentAsJson(route(app, signIn).get) \ "access_token").as[String]
    (publicId, token)
  }

  private def graphql(body: JsValue, token: Option[String]) = {
    val request = FakeRequest(POST, "/graphql").withJsonBody(body)
    route(app, token.fold(request)(t => request.withHeaders("Authorization" -> s"Bearer $t"))).get
  }

  "POST /graphql" must {
    "answer me with the caller's public id and role" in {
      val (publicId, token) = signUpAndSignIn()

      val result = graphql(Json.obj("query" -> "{ me { publicId role } }"), Some(token))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("data" -> Json.obj("me" -> Json.obj("publicId" -> publicId, "role" -> "CLIENT")))
    }

    "return 401 with no token" in {
      val result = graphql(Json.obj("query" -> "{ me { publicId role } }"), None)

      status(result) mustBe UNAUTHORIZED
      (contentAsJson(result) \ "error" \ "code").as[String] mustBe "UNAUTHORIZED"
    }

    "return 400 VALIDATION_ERROR for a body without query" in {
      val (_, token) = signUpAndSignIn()

      val result = graphql(Json.obj("variables" -> Json.obj()), Some(token))

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "error" \ "code").as[String] mustBe "VALIDATION_ERROR"
    }
  }
}
