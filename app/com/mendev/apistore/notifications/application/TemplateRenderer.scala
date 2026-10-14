package com.mendev.apistore.notifications.application

import com.mendev.apistore.shared.error.AppError

trait TemplateRenderer {
  // Left for an unknown template name or data the template cannot use.
  def render(template: String, recipientEmail: String, data: Map[String, String],
             secret: Option[String]): Either[AppError, EmailMessage]
}
