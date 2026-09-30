package shared.config

import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

final case class DatabaseConfig(
                                 host: String,
                                 port: Int,
                                 name: String,
                                 user: String,
                                 password: Secret
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
                            mail: MailConfig
                          )

object AppConfig {

  def load(source: ConfigSource): Either[ConfigReaderFailures, AppConfig] =
    source.at("app").load[AppConfig]
}