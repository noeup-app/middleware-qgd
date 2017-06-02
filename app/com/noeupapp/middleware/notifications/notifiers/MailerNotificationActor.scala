package com.noeupapp.middleware.notifications.notifiers

import java.util.UUID

import akka.actor.{Actor, Props}
import com.google.inject.Inject
import com.noeupapp.middleware.entities.user.UserService
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.notifications.NotificationMessage
import com.noeupapp.middleware.utils.mailer.{EmailTemplate, MessageEmail}
import com.noeupapp.middleware.utils.TypeCustom._
import play.api.Logger
import com.noeupapp.middleware.utils.FutureFunctor._

import scala.concurrent.Future
import scalaz.EitherT

/**
  * Created by damien on 30/05/2017.
  */
class MailerNotificationActor(mailerNotificationService: MailerNotificationService) extends Actor {
  override def receive: Receive = {
    case NotificationMessage(notificationId, userId, message_type, message_data) =>


      mailerNotificationService.sendMail(userId, message_type, message_data)
  }
}

object MailerNotificationActor {
  def props = Props[MailerNotificationActor]
}



class MailerNotificationService @Inject()(emailTemplate: EmailTemplate,
                                          mailerNotificationConfiguration: MailerNotificationConfiguration,
                                          messageEmail: MessageEmail,
                                          userService: UserService) {


  def sendMail(targetId: UUID, messageType: String, messageData: Any): Future[Expect[String]] = {
    for{
      userOpt <- EitherT(userService.findById(targetId))
      email   <- EitherT(userOpt.flatMap(_.email) |> "User not found or email not found")
      res     <- EitherT(sendMail(email, messageType, messageData))
    } yield res
  }.run


  private def sendMail(targetEmail: String, messageType: String, messageData: Any): Future[Expect[String]] = {

    mailerNotificationConfiguration.content(messageType, messageData.toString) match {
      case Some((subject, emailContent)) =>
        messageEmail.sendEmail(
          senderName = Some(emailTemplate.getSenderName),
          senderEmail = emailTemplate.getSenderEmail,
          receiverName = targetEmail,
          receiverEmail = targetEmail,
          subject = s"${emailTemplate.getAppName} - $subject",
          text = emailContent,
          appName = emailTemplate.getAppName
        )
      case _ =>
    }

  }


}
