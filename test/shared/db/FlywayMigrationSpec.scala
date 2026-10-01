package shared.db

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FlywayMigrationSpec extends AnyWordSpec with Matchers {

  "Flyway on an empty MySQL" should {
    "create the shared_event table" in {
      val tables = TestDatabase.query("SHOW TABLES")(_.getString(1))
      tables should contain("shared_event")
    }

    "record migration 1 as successful" in {
      val rows = TestDatabase.query("SELECT version, success FROM flyway_schema_history")(r => (r.getString(1), r.getBoolean(2)))
      rows should contain(("1", true))
    }
  }
}