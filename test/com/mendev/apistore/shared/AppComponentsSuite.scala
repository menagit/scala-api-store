package com.mendev.apistore.shared

import com.mendev.apistore.AppComponents
import com.mendev.apistore.shared.db.TestDatabase
import org.apache.pekko.stream.Materializer
import org.scalatest.TestSuite
import org.scalatestplus.play.components.OneAppPerSuiteWithComponents
import play.api.{ApplicationLoader, BuiltInComponents, Environment}

trait AppComponentsSuite extends OneAppPerSuiteWithComponents { this: TestSuite =>

  implicit lazy val materializer: Materializer = app.materializer

  override def components: BuiltInComponents = {
    val db  = TestDatabase.config
    val url = s"jdbc:mysql://${db.host}:${db.port}/${db.name}"

    val context = ApplicationLoader.Context.create(
      Environment.simple(),
      initialSettings = Map(
        "app.database.host"     -> db.host,
        "app.database.port"     -> db.port.toString,
        "app.database.name"     -> db.name,
        "app.database.user"     -> db.user,
        "app.database.password" -> db.password.value,
        "db.default.url"        -> url,
        "db.default.username"   -> db.user,
        "db.default.password"   -> db.password.value,
        "app.mail.host"         -> "localhost",
        "app.mail.port"         -> "1025",
        "app.mail.user"         -> "",
        "app.mail.password"     -> "",
        "app.mail.from"         -> "test@apistore.local",
        "app.jwt.secret"        -> "test-secret-test-secret-test-secret-1234",
        "app.jwt.access-token-ttl" -> "2 minutes",
        "app.encryption.key"       -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.mail.link-base-url"   -> "http://localhost:3000",
      )
    )

    new AppComponents(context)
  }
}
