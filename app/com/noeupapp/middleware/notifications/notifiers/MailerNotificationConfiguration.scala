package com.noeupapp.middleware.notifications.notifiers

/**
  * Created by damien on 01/06/2017.
  */
trait MailerNotificationConfiguration {

  def content(messageType: String, messageData: String): Option[(String, String)]

}
