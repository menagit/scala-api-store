package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class GetUserContactUseCase(userRepo: UserRepository) {

  def executeIn(userPublicId: String, conn: Connection): Either[AppError, Option[UserContact]] =
    userRepo.findByPublicId(userPublicId, conn).map(_.map(u => UserContact(u.email, u.firstName)))
}
