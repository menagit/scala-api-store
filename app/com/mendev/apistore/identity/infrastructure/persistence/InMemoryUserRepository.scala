package com.mendev.apistore.identity.infrastructure.persistence

import com.mendev.apistore.identity.application.UserRepository
import com.mendev.apistore.identity.domain.User
import com.mendev.apistore.shared.error.AppError
import jakarta.inject.Singleton

import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

// Temp... then we'll replace it with a relate repo...
@Singleton
class InMemoryUserRepository extends UserRepository {

  private val users = new ConcurrentHashMap[String, User]()

  override def save(user: User, conn: Connection): Either[AppError, Unit] = {
    Option(users.putIfAbsent(user.email, user)) match {
      case None    => Right(())
      case Some(_) => Left(AppError.Conflict("Email is already registered"))
    }
  }
}