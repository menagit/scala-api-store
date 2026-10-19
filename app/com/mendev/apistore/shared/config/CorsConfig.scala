package com.mendev.apistore.shared.config

import java.net.URI
import scala.util.Try

final case class CorsConfig(allowedOrigins: String = "") {

  val origins: List[String] = allowedOrigins.split(",").map(_.trim).filter(_.nonEmpty).toList

  require(
    origins.forall(CorsConfig.isOrigin),
    "Each CORS allowed origin must be an http or https origin like http://localhost:8000, with no path and no *"
  )
}

object CorsConfig {

  private def isOrigin(value: String): Boolean =
    Try {
      val uri = new URI(value)
      (uri.getScheme == "http" || uri.getScheme == "https") &&
        uri.getHost != null &&
        uri.getRawPath.isEmpty &&
        uri.getRawQuery == null &&
        uri.getRawFragment == null &&
        uri.getRawUserInfo == null
    }.getOrElse(false)
}
