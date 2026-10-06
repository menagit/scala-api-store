package com.mendev.apistore.shared.config

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import pureconfig.ConfigSource

class ConfigModule(environment: Environment, configuration: Configuration)
  extends AbstractModule {

  override def configure(): Unit = {
    val appConfig = AppConfig.load(ConfigSource.fromConfig(configuration.underlying)) match {
      case Right(config) => config
      case Left(failures) =>
        throw new IllegalStateException(
          s"Invalid configuration:\n${failures.prettyPrint()}"
        )
    }

    bind(classOf[AppConfig]).toInstance(appConfig)
    bind(classOf[DatabaseConfig]).toInstance(appConfig.database)
    bind(classOf[MailConfig]).toInstance(appConfig.mail)
    bind(classOf[JwtConfig]).toInstance(appConfig.jwt)
  }
}