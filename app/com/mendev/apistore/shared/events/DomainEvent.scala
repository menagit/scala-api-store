package com.mendev.apistore.shared.events

// Something that happened in one context and that another context may react to.
// The adapter turns the payload (only plain strings) into JSON.
trait DomainEvent {
  def eventType: String
  def payload: Map[String, String]
}
