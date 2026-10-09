package com.mendev.apistore

import com.mendev.apistore.identity.IdentityComponents
import com.mendev.apistore.shared.SharedComponents
import com.mendev.apistore.shared.web.JsonErrorHandler
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.http.HttpErrorHandler
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import _root_.router.Routes
import com.mendev.apistore.notifications.NotificationsComponents
import play.api.mvc.EssentialFilter
import com.mendev.apistore.notifications.application.EmailMessage

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with SharedComponents
    with IdentityComponents
    with NotificationsComponents
 {

  // Run the migrations now, before the app takes requests (was asEagerSingleton).
  flywayMigrator
  //Starts the cleanUp (hourly)
  tokenCleanupJob

  override lazy val httpErrorHandler: HttpErrorHandler = new JsonErrorHandler

  // Arguments follow the order in conf/routes: error handler, then each controller once.
  lazy val router: Router = new Routes(httpErrorHandler, healthController, authController)

  // No Play CSRF filter: this is a token-less JSON API. 
  override def httpFilters: Seq[EssentialFilter] =
    super.httpFilters.filterNot(_ == csrfFilter)
}