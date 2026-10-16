package com.mendev.apistore.graphql

import com.mendev.apistore.shared.config.GraphQLConfig
import com.mendev.apistore.shared.error.{AppError, FieldError}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import sangria.execution.{ExceptionHandler, Executor, HandledException, MaxQueryDepthReachedError, QueryAnalysisError, QueryReducer, QueryReducingError}
import sangria.marshalling.ResultMarshaller
import sangria.marshalling.playJson.*
import sangria.parser.QueryParser
import sangria.schema.Schema

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class GraphQLExecutor(schema: Schema[GraphQLContext, Unit], config: GraphQLConfig)(implicit ec: ExecutionContext) {

  private val logger = Logger(getClass)

  def execute(request: GraphQLRequest, context: GraphQLContext): Future[JsValue] =
    QueryParser.parse(request.query) match {
      case Failure(_) => Future.successful(parseFailed)
      case Success(document) =>
        Executor
          .execute(
            schema,
            document,
            context,
            operationName = request.operationName,
            variables = request.variables.getOrElse(Json.obj()),
            exceptionHandler = exceptionHandler,
            queryReducers = List(
              QueryReducer.rejectMaxDepth[GraphQLContext](config.maxDepth),
              QueryReducer.rejectComplexQueries[GraphQLContext](config.maxComplexity, (_, _) => new QueryTooComplexException)
            )
          )
          .recover { case error: QueryAnalysisError => error.resolveError }
    }

  private val parseFailed: JsValue = Json.obj(
    "errors" -> Json.arr(
      Json.obj(
        "message"    -> "Invalid query syntax",
        "extensions" -> Json.obj("code" -> "GRAPHQL_PARSE_FAILED", "details" -> Json.arr())
      )
    )
  )

  private val exceptionHandler = ExceptionHandler(
    onException = {
      case (m, AppErrorException(error))      => handled(m, error.code, error.message, details(error))
      case (m, QueryReducingError(_: MaxQueryDepthReachedError, _)) => tooComplex(m)
      case (m, QueryReducingError(_: QueryTooComplexException, _))  => tooComplex(m)
      case (m, e) =>
        logger.error("Unexpected error in a GraphQL resolver", e)
        val error = AppError.Unexpected()
        handled(m, error.code, error.message)
    },
    onViolation = { case (m, violation) =>
      handled(m, "GRAPHQL_VALIDATION_FAILED", violation.errorMessage)
    }
  )

  private def tooComplex(m: ResultMarshaller): HandledException =
    handled(m, "QUERY_TOO_COMPLEX", "Query is too complex")

  private def details(error: AppError): List[FieldError] = error match {
    case AppError.Validation(_, fields) => fields
    case _                              => Nil
  }

  private def handled(m: ResultMarshaller, code: String, message: String, details: List[FieldError] = Nil): HandledException =
    HandledException(
      message,
      Map(
        "code" -> m.scalarNode(code, "String", Set.empty),
        "details" -> m.arrayNode(details.toVector.map { f =>
          m.mapNode(Seq("field" -> m.scalarNode(f.field, "String", Set.empty), "message" -> m.scalarNode(f.message, "String", Set.empty)))
        })
      )
    )
}
