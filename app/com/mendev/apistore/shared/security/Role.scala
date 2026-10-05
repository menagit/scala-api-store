package com.mendev.apistore.shared.security

sealed trait Role

case object Manager extends Role
case object Client extends Role