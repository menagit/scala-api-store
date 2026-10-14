package com.mendev.apistore.identity.application

final case class ResetPasswordCommand(token: String, newPwd: String)