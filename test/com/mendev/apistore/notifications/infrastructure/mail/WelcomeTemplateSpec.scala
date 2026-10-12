package com.mendev.apistore.notifications.infrastructure.mail

import com.mendev.apistore.notifications.infrastructure.mail.views.html.{welcome => welcomeHtml}
import com.mendev.apistore.notifications.infrastructure.mail.views.txt.{welcome => welcomeTxt}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class WelcomeTemplateSpec extends AnyWordSpec with Matchers {

  private val link = "https://testExample.com/confirm"

  "The HTML welcome template" should {
    "show the name and the link inside the layout" in {
      val body = welcomeHtml("Ana", link).body

      body should include("Welcome, Ana")
      body should include(s"""href="$link"""")
      body should include("<title>Welcome to API Store</title>")
    }

    "escape markup in the name" in {
      val body = welcomeHtml("<b>Ana</b>", link).body

      body should include("&lt;b&gt;Ana&lt;/b&gt;")
      body should not include "<b>Ana</b>"
    }
  }

  "The text welcome template" should {
    "show the name and the link" in {
      val body = welcomeTxt("Ana", link).body

      body should include("Welcome, Ana")
      body should include(link)
    }

    "not escape the name" in {
      welcomeTxt("<b>Ana</b>", link).body should include("<b>Ana</b>")
    }
  }
}