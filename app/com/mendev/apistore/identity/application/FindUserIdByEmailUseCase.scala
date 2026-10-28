package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class FindUserIdByEmailUseCase(userRepo: UserRepository) {

  def executeIn(email: String, conn: Connection): Either[AppError, Option[String]] =
    userRepo.findByEmail(email, conn).map(_.map(_.publicId))
}
