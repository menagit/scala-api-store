package com.mendev.apistore.notifications.application

final case class EmailMessage (to: String, subject: String, text: String,
                        html:Option[String]= None){
  override def toString: String = s"EmailMessage($to, $subject)"
}
