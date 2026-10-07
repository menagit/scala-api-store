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

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with SharedComponents
    with IdentityComponents {

  // Run the migrations now, before the app takes requests (was asEagerSingleton).
  flywayMigrator

  override lazy val httpErrorHandler: HttpErrorHandler = new JsonErrorHandler

  // Arguments follow the order in conf/routes: error handler, then each controller once.
  lazy val router: Router = new Routes(httpErrorHandler, healthController, authController)
}