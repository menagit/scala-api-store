package com.mendev.apistore.identity

import com.google.inject.AbstractModule
import com.mendev.apistore.identity.application.{IdGenerator, PasswordHasher, UserRepository}
import com.mendev.apistore.identity.infrastructure.persistence.InMemoryUserRepository
import com.mendev.apistore.identity.infrastructure.PlainTextPasswordHasher
import com.mendev.apistore.identity.infrastructure.id.UuidV7Generator


class IdentityModule extends AbstractModule{

  override def configure(): Unit = {
    // PlainTextPasswordHasher and InMemoryUserRepository are temps....
    bind(classOf[IdGenerator]).to(classOf[UuidV7Generator])
    bind(classOf[PasswordHasher]).to(classOf[PlainTextPasswordHasher])
    bind(classOf[UserRepository]).to(classOf[InMemoryUserRepository])
  }

}
