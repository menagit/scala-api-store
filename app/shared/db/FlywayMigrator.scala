package shared.db

import jakarta.inject.{Inject, Singleton}

import org.flywaydb.core.Flyway
import play.api.Logging
import shared.config.DatabaseConfig

@Singleton
class FlywayMigrator @Inject() (db: DatabaseConfig) extends Logging {

  migrate()

  private def migrate(): Unit = {
    val url = s"jdbc:mysql://${db.host}:${db.port}/${db.name}"
    val result = Flyway
      .configure()
      .dataSource(url, db.user, db.password.value)
      .locations("classpath:db/migration")
      .load()
      .migrate()
    logger.info(s"Flyway: ${result.migrationsExecuted} migration(s) applied")
  }
}