package com.mendev.apistore.notifications.infrastructure.events

import com.mendev.apistore.notifications.application.{EventDispatcher, EventHandler, NewDelivery, StoredEvent}
import com.mendev.apistore.shared.error.AppError
import play.api.libs.json.{Json, Reads}

import scala.util.Try
import scala.util.control.NonFatal

// One event type, the way to read its payload, and its handler.
// The type parameter E is hidden here, so routes for different events fit in one list.
final class EventRoute private (
                                 val eventType: String,
                                 val run: String => Either[AppError, List[NewDelivery]]
                               )

object EventRoute {
  def apply[E: Reads](eventType: String, handler: EventHandler[E]): EventRoute =
    new EventRoute(eventType, payload => read[E](payload).flatMap(handler.handle))

  // The error never contains the payload: it has personal data.
  private def read[E: Reads](payload: String): Either[AppError, E] =
    Try(Json.parse(payload)).toOption
      .flatMap(_.asOpt[E])
      .toRight(AppError.Validation("Unreadable event payload"))
}

class RoutingEventDispatcher(routes: List[EventRoute]) extends EventDispatcher {

  // Two routes for one event type is a wiring bug.
  require(
    routes.map(_.eventType).distinct.size == routes.size,
    "Duplicate event type in the notification routes"
  )

  private val byType: Map[String, EventRoute] = routes.map(r => r.eventType -> r).toMap

  override def fanOut(event: StoredEvent): Option[Either[AppError, List[NewDelivery]]] =
    byType.get(event.eventType).map { route =>
      //One handler bug must not roll back the whole batch
      try route.run(event.payload)
      catch { case NonFatal(_) => Left(AppError.Unexpected("Event handler failed")) }
    }
}