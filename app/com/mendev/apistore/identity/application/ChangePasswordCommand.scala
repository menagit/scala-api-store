package com.mendev.apistore.identity.application

final case class ChangePasswordCommand(currentPwd:String, newPwd:String)
