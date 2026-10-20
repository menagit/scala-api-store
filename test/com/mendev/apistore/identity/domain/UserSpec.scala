package com.mendev.apistore.identity.domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UserSpec extends AnyWordSpec with Matchers {

  "User" should {
    "normalize an email and accept only .com and .co emails of up to 254 characters" in {
      User.normalizeEmail("  Ana@Example.COM ") shouldBe "ana@example.com"
      User.isValidEmail("a@b.com") shouldBe true
      User.isValidEmail("a@b.co") shouldBe true
      User.isValidEmail("a@b.org") shouldBe false
      User.isValidEmail("ab.com") shouldBe false
      User.isValidEmail("a" * 249 + "@b.com") shouldBe false
    }

    "accept passwords of 8 to 16 characters" in {
      User.isValidPassword("a" * 7) shouldBe false
      User.isValidPassword("a" * 8) shouldBe true
      User.isValidPassword("a" * 16) shouldBe true
      User.isValidPassword("a" * 17) shouldBe false
    }
  }
}
