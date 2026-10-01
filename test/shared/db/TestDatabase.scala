package shared.db

import java.sql.{DriverManager, ResultSet}

import com.dimafeng.testcontainers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import shared.config.{DatabaseConfig, Secret}

object TestDatabase {

  private lazy val mysql: MySQLContainer = {
    val c = MySQLContainer(mysqlImageVersion = DockerImageName.parse("mysql:8.4"))
    c.start()
    sys.addShutdownHook(c.stop())
    c
  }

  lazy val config: DatabaseConfig = {
    val j = mysql.container
    val cfg = DatabaseConfig(
      host = j.getHost,
      port = j.getMappedPort(3306),
      name = j.getDatabaseName,
      user = j.getUsername,
      password = Secret(j.getPassword)
    )
    new FlywayMigrator(cfg)
    cfg
  }

  def query[A](sql: String)(read: ResultSet => A): List[A] = {
    val conn = DriverManager.getConnection(
      s"jdbc:mysql://${config.host}:${config.port}/${config.name}",
      config.user,
      config.password.value
    )
    try {
      val rs = conn.createStatement().executeQuery(sql)
      Iterator.continually(rs).takeWhile(_.next()).map(read).toList
    } finally conn.close()
  }
}