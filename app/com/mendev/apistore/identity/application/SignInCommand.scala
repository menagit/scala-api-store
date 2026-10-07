package com.mendev.apistore.identity.application

final case class SignInCommand(email: String, password: String)