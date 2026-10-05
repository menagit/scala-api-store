package com.mendev.apistore.shared.db

import com.dimafeng.testcontainers.MySQLContainer
import com.mendev.apistore.shared.config.{DatabaseConfig, Secret}
import org.testcontainers.utility.DockerImageName

import java.sql.{DriverManager, ResultSet}

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
    val conn =  connect()
    try {
      val rs = conn.createStatement().executeQuery(sql)
      Iterator.continually(rs).takeWhile(_.next()).map(read).toList
    } finally conn.close()
  }

  private def connect() = DriverManager.getConnection(
    s"jdbc:mysql://${config.host}:${config.port}/${config.name}",
    config.user,
    config.password.value
  )

  def update(sql: String): Unit = {
    val conn = connect()
    try {
      val st = conn.createStatement()
      st.executeUpdate(sql)
      st.close()
    } finally conn.close()
  }
}