package com.mendev.apistore.notifications

import com.mendev.apistore.notifications.application.*
import com.mendev.apistore.notifications.infrastructure.events.RoutingEventDispatcher
import com.mendev.apistore.notifications.infrastructure.jobs.OutboxPollerJob
import com.mendev.apistore.notifications.infrastructure.mail.{PlayMailerEmailSender, TwirlTemplateRenderer}
import com.mendev.apistore.notifications.infrastructure.persistence.{RelateDeliveryRepository, RelateEventRepository}
import com.mendev.apistore.shared.SharedComponents
import play.api.ContextBasedBuiltInComponents
import play.api.libs.mailer.MailerComponents

trait NotificationsComponents extends MailerComponents {
  this: ContextBasedBuiltInComponents with SharedComponents =>   // Play basics + shared (allowed for every context)

  // Sending mail blocks like JDBC,
  // so it runs on the db-dispatcher pool and not on Play's request threads.
  lazy val emailSender: EmailSender =
    new PlayMailerEmailSender(mailerClient, appConfig.mail.from)(actorSystem.dispatchers.lookup("db-dispatcher"))

  lazy val eventRepository: EventRepository       = new RelateEventRepository
  lazy val deliveryRepository: DeliveryRepository = new RelateDeliveryRepository
  lazy val templateRenderer: TemplateRenderer     = new TwirlTemplateRenderer

  // No routes yet: every event is skipped until the first handler is added (T2.8.2).
  lazy val eventDispatcher: EventDispatcher = new RoutingEventDispatcher(Nil)

  lazy val fanOutEventsUseCase: FanOutEventsUseCase =
    new FanOutEventsUseCase(eventRepository, deliveryRepository, eventDispatcher, txRunner, clock)

  lazy val sendDeliveriesUseCase: SendDeliveriesUseCase =
    new SendDeliveriesUseCase(deliveryRepository, templateRenderer, emailSender, txRunner, clock)(executionContext)

  lazy val outboxPollerJob: OutboxPollerJob =
    new OutboxPollerJob(fanOutEventsUseCase, sendDeliveriesUseCase, actorSystem, applicationLifecycle)(executionContext)

  override def config: com.typesafe.config.Config = configuration.underlying
}