package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError

import scala.concurrent.Future

trait EmailSender {
  //Left means mail server couldn't take the message and the error is
  //returned by the adapter
  def send(emailMessage:EmailMessage): Future[Either[AppError,Unit]]
}
