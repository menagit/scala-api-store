package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.config.Secret

//Called by the controller to build the cookie
final case class IssuedRefreshToken(value: Secret, maxAgeSeconds: Long)