package com.mendev.apistore.graphql

import com.mendev.apistore.shared.config.GraphQLConfig
import com.mendev.apistore.shared.error.AppError
import com.mendev.apistore.shared.security.{Actor, Client}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsNull, JsValue}
import sangria.introspection.introspectionQueryString
import sangria.schema.{Field, ObjectType, OptionType, Schema, StringType, fields}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class GraphQLExecutorSpec extends AnyWordSpec with Matchers {

  private val queryType = ObjectType(
    "Query",
    fields[GraphQLContext, Unit](
      Field("ok", StringType, resolve = _ => "ok"),
      Field(
        "forbidden",
        OptionType(StringType),
        resolve = _ => AppErrorException.orFail[Option[String]](Future.successful(Left(AppError.Forbidden())))
      )
    )
  )

  private val executor = new GraphQLExecutor(Schema(queryType), GraphQLConfig())
  private val context  = GraphQLContext(Actor("0192f5a0-0000-7000-8000-000000000001", Client))

  private def run(query: String): JsValue =
    Await.result(executor.execute(GraphQLRequest(query, None, None), context), 5.seconds)

  private def firstCode(json: JsValue): String =
    (json \ "errors" \ 0 \ "extensions" \ "code").as[String]

  "GraphQLExecutor" should {
    "put FORBIDDEN in extensions when a field fails with AppError.Forbidden" in {
      val json = run("{ ok forbidden }")

      firstCode(json) shouldBe "FORBIDDEN"
      (json \ "data" \ "forbidden").get shouldBe JsNull
      (json \ "data" \ "ok").as[String] shouldBe "ok"
    }

    "answer GRAPHQL_PARSE_FAILED for broken syntax" in {
      firstCode(run("{ ok ")) shouldBe "GRAPHQL_PARSE_FAILED"
    }

    "answer GRAPHQL_VALIDATION_FAILED for an unknown field" in {
      firstCode(run("{ unknown }")) shouldBe "GRAPHQL_VALIDATION_FAILED"
    }

    "run the standard introspection query within the limits" in {
      val json = run(introspectionQueryString())

      (json \ "errors").toOption shouldBe None
      (json \ "data" \ "__schema").toOption shouldBe defined
    }

    "answer QUERY_TOO_COMPLEX for a query deeper than 15" in {
      val query = "{ __schema { types { " + "ofType { " * 13 + "name" + " }" * 15 + " }"

      firstCode(run(query)) shouldBe "QUERY_TOO_COMPLEX"
    }
  }
}
