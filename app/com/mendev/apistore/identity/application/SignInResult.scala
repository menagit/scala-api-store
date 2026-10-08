package com.mendev.apistore.identity.application

final case class SignInResult(accessToken: IssuedToken, refreshToken: IssuedRefreshToken)