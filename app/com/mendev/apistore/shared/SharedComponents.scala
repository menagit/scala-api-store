package com.mendev.apistore.shared

import com.mendev.apistore.shared.actions.RateLimitedAction
import com.mendev.apistore.shared.config.AppConfig
import com.mendev.apistore.shared.db.{FlywayMigrator, PlayDbTxRunner, TxRunner}
import com.mendev.apistore.shared.health.HealthCheck
import com.mendev.apistore.shared.health.web.HealthController
import com.mendev.apistore.shared.security.RateLimiter
import play.api.ContextBasedBuiltInComponents
import play.api.db.{DBComponents, HikariCPComponents}
import pureconfig.ConfigSource

import java.time.Clock

trait SharedComponents extends DBComponents with HikariCPComponents {
  this: ContextBasedBuiltInComponents =>   // gives configuration, actorSystem, executionContext, controllerComponents

  lazy val appConfig: AppConfig =
    AppConfig.load(ConfigSource.fromConfig(configuration.underlying)) match {
      case Right(config) => config
      case Left(failures) =>
        throw new IllegalStateException(s"Invalid configuration:\n${failures.prettyPrint()}")
    }

  lazy val clock: Clock = Clock.systemUTC()

  // Look up the pool by name, so JDBC work never runs on Play's request threads.
  lazy val txRunner: TxRunner =
    new PlayDbTxRunner(dbApi.database("default"), actorSystem.dispatchers.lookup("db-dispatcher"))

  // Lazy here; AppComponents forces it so the startup order is decided in one place.
  lazy val flywayMigrator: FlywayMigrator = new FlywayMigrator(appConfig.database)

  lazy val rateLimiter: RateLimiter             = new RateLimiter
  lazy val rateLimitedAction: RateLimitedAction = new RateLimitedAction(rateLimiter)(executionContext)

  lazy val healthCheck: HealthCheck           = new HealthCheck(txRunner)
  lazy val healthController: HealthController = new HealthController(controllerComponents, healthCheck)(executionContext)
}