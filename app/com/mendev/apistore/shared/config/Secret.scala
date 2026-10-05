package com.mendev.apistore.shared.config

import pureconfig.ConfigReader

final case class Secret(value: String) extends AnyVal {
  override def toString: String = "Secret(***)"
}

object Secret {
  implicit val reader: ConfigReader[Secret] =
    ConfigReader[String].map(Secret(_))
}
