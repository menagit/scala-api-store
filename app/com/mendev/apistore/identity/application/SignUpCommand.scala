package com.mendev.apistore.identity.application

case class SignUpCommand(
    email: String,
    firstName: String,
    lastName: String,
    password: String
)
