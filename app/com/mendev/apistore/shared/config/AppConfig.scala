package com.mendev.apistore.shared.config

import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto.*
import com.mendev.apistore.shared.config.EncryptionConfig
import java.net.URI
import scala.util.Try

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
                             from: String,
                             linkBaseUrl: String // front-end address used in email links
                           ) {

  require(
    MailConfig.isValidBaseUrl(linkBaseUrl),
    "The link base URL must be an http or https address with no trailing slash"
  )
}

final case class AppConfig(
                            database: DatabaseConfig,
                            mail: MailConfig,
                            jwt: JwtConfig,
                            encryption: EncryptionConfig,
                            graphql: GraphQLConfig = GraphQLConfig(),
                            cors: CorsConfig = CorsConfig()
                          )

object AppConfig {

  def load(source: ConfigSource): Either[ConfigReaderFailures, AppConfig] =
    source.at("app").load[AppConfig]
}

object MailConfig {

  private def isValidBaseUrl(value: String): Boolean =
    Try {
      val uri = new URI(value)
      (uri.getScheme == "http" || uri.getScheme == "https") &&
        uri.getHost != null &&
        !value.endsWith("/")
    }.getOrElse(false)
}