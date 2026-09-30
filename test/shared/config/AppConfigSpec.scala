package shared.config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pureconfig.ConfigSource

class AppConfigSpec extends AnyWordSpec with Matchers {

  private val mail =
    """mail { host = "localhost", port = 1025, user = "", password = "", from = "no-reply@apistore.local" }"""

  private val valid =
    s"""app {
       |  database { host = "localhost", port = 3306, name = "api_store", user = "app", password = "db-secret" }
       |  $mail
       |}""".stripMargin

  private val broken =
    s"""app {
       |  database { host = "localhost", port = "abc", name = "api_store", user = "app" }
       |  $mail
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
  }
}