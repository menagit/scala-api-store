package com.mendev.apistore.shared.config

import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto.*

final case class DatabaseConfig(
                                 host: String,
                                 port: Int,
                                 name: String,
                                 user: String,
                                 password: Secret,
                                 poolSize: Int = 10
                               )

final case class MailConfig(
                             host: String,
                             port: Int,
                             user: String,
                             password: Secret,
                             from: String
                           )

final case class AppConfig(
                            database: DatabaseConfig,
                            mail: MailConfig,
                            jwt: JwtConfig
                          )

object AppConfig {

  def load(source: ConfigSource): Either[ConfigReaderFailures, AppConfig] =
    source.at("app").load[AppConfig]
}