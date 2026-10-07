package com.mendev.apistore.identity.application

import com.mendev.apistore.identity.domain.User

trait TokenIssuer {
  def issue(user: User): IssuedToken
}
