package com.mendev.apistore.identity.application

import com.mendev.apistore.shared.error.AppError

import java.sql.Connection

class GetUserSummariesUseCase(userRepo: UserRepository) {

  def executeIn(publicIds: List[String], conn: Connection): Either[AppError, List[UserSummary]] =
    userRepo.findByPublicIds(publicIds, conn).map(_.map(u => UserSummary(u.publicId, u.email, u.firstName, u.lastName)))
}
