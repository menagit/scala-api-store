package com.mendev.apistore

import com.mendev.apistore.catalog.CatalogComponents
import com.mendev.apistore.graphql.GraphQLComponents
import com.mendev.apistore.identity.IdentityComponents
import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shopping.ShoppingComponents
import com.mendev.apistore.shared.web.JsonErrorHandler
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.http.HttpErrorHandler
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import play.filters.cors.{CORSComponents, CORSConfig}
import _root_.router.Routes
import com.mendev.apistore.notifications.NotificationsComponents
import play.api.mvc.EssentialFilter
import com.mendev.apistore.notifications.application.{Recipient, RecipientDirectory}
import com.mendev.apistore.ordering.OrderingComponents
import com.mendev.apistore.ordering.application.OrderIdGenerator
import java.util.UUID

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with CORSComponents
    with SharedComponents
    with IdentityComponents
    with CatalogComponents
    with ShoppingComponents
    with OrderingComponents
    with NotificationsComponents
    with GraphQLComponents
 {

  // Run the migrations now, before the app takes requests (was asEagerSingleton).
  flywayMigrator
  //Starts the cleanUp (hourly)
  tokenCleanupJob
   //Starts the outbox poller (every 30 seconds)
   outboxPollerJob

  override lazy val httpErrorHandler: HttpErrorHandler = new JsonErrorHandler

  lazy val orderIdGenerator: OrderIdGenerator = () => UUID.fromString(idGenerator.generatePublicId())

  lazy val recipientDirectory: RecipientDirectory =
    (publicId, conn) => getUserContactUseCase.executeIn(publicId, conn).map(_.map(c => Recipient(c.email, c.firstName)))

  // Arguments follow the order in conf/routes: error handler, then each controller once.
  lazy val router: Router = new Routes(httpErrorHandler, healthController, authController, meController, productController, graphQLController)

  override lazy val corsConfig: CORSConfig = CORSConfig(
    allowedOrigins = CORSConfig.Origins.Matching(appConfig.cors.origins.toSet),
    isHttpMethodAllowed = Set("GET", "POST", "OPTIONS"),
    isHttpHeaderAllowed = Set("authorization", "content-type"),
    supportsCredentials = false
  )

  // No Play CSRF filter: this is a token-less JSON API.
  override def httpFilters: Seq[EssentialFilter] =
    corsFilter +: super.httpFilters.filterNot(_ == csrfFilter)
}