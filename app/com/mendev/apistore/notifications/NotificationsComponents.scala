package com.mendev.apistore.notifications

import com.mendev.apistore.notifications.application.EmailSender
import com.mendev.apistore.notifications.infrastructure.mail.PlayMailerEmailSender
import com.mendev.apistore.shared.SharedComponents
import play.api.ContextBasedBuiltInComponents
import play.api.libs.mailer.MailerComponents

trait NotificationsComponents extends MailerComponents{
  this: ContextBasedBuiltInComponents with SharedComponents =>   // Play basics + shared (allowed for every context)

  // Sending mail blocks like JDBC,
  // so it runs on the db-dispatcher pool and not on Play's request threads.
  lazy val emailSender: EmailSender =
    new PlayMailerEmailSender(mailerClient, appConfig.mail.from)(actorSystem.dispatchers.lookup("db-dispatcher"))

  override def config: com.typesafe.config.Config = configuration.underlying
}