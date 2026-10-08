package com.mendev.apistore.shared.config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pureconfig.ConfigSource

import scala.concurrent.duration.*

class AppConfigSpec extends AnyWordSpec with Matchers {

  private val mail =
    """mail { host = "localhost", port = 1025, user = "", password = "", from = "no-reply@apistore.local" }"""

  private val jwt =
    """jwt { secret = "test-secret-test-secret-test-secret-1234", access-token-ttl = "2 minutes", refresh-token-ttl = "30 days" }"""

  private val valid =
    s"""app {
       |  database { host = "localhost", port = 3306, name = "api_store", user = "app", password = "db-secret" }
       |  $mail
       |  $jwt
       |}""".stripMargin

  private val broken =
    s"""app {
       |  database { host = "localhost", port = "abc", name = "api_store", user = "app" }
       |  $mail
       |  $jwt
       |}""".stripMargin

  private val withPoolSize =
    s"""app {
       |  database { host = "localhost", port = 3306, name = "api_store", user = "app", password = "db-secret", pool-size = 25 }
       |  $mail
       |  $jwt
       |}""".stripMargin

  private val withShortSecret =
    s"""app {
       |  database { host = "localhost", port = 3306, name = "api_store", user = "app", password = "db-secret" }
       |  $mail
       |  jwt { secret = "too-short", access-token-ttl = "2 minutes" , refresh-token-ttl = "30 days"}
       |}""".stripMargin

  "AppConfig.load" should {

    "load a valid configuration" in {
      val result = AppConfig.load(ConfigSource.string(valid))

      result.map(_.database.port) shouldBe Right(3306)
      result.map(_.mail.from) shouldBe Right("no-reply@apistore.local")
    }

    "report every problem together when the configuration is bad" in {
      AppConfig.load(ConfigSource.string(broken)) match {
        case Left(failures) =>
          val text = failures.prettyPrint()
          text should include("port")
          text should include("password")
        case Right(_) =>
          fail("Expected the bad configuration to be rejected")
      }
    }

    "never show passwords when the configuration is printed" in {
      val printed = AppConfig.load(ConfigSource.string(valid)).map(_.toString)

      printed.map(_.contains("db-secret")) shouldBe Right(false)
    }

    "use a pool size of 10 when the key is missing" in {
      AppConfig.load(ConfigSource.string(valid)).map(_.database.poolSize) shouldBe Right(10)
    }

    "read the pool size when it is set" in {
      AppConfig.load(ConfigSource.string(withPoolSize)).map(_.database.poolSize) shouldBe Right(25)
    }

    "read the access token lifetime" in {
      AppConfig.load(ConfigSource.string(valid)).map(_.jwt.accessTokenTtl) shouldBe Right(2.minutes)
    }

    "never show the jwt secret when the configuration is printed" in {
      val printed = AppConfig.load(ConfigSource.string(valid)).map(_.toString)

      printed.map(_.contains("test-secret")) shouldBe Right(false)
    }

    "reject a jwt secret shorter than 32 bytes" in {
      an[IllegalArgumentException] should be thrownBy
        AppConfig.load(ConfigSource.string(withShortSecret))
    }
  }
}