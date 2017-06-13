package com.noeupapp.middleware.entities.user.email

import com.google.inject.Inject
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.mailer.{EmailTemplate, MessageEmail}

import scala.concurrent.Future

/**
  * Created by damien on 13/06/2017.
  */
class UpdateEmailEmailSender @Inject()(updateEmailEmailFactory: UpdateEmailEmailFactory,
                                       messageEmail: MessageEmail,
                                       emailTemplate: EmailTemplate){


  def sendUpdateMailRequest(user: User, token: String, newEmail: String): Future[Expect[String]] = {
    val (subject, content) = updateEmailEmailFactory.getUpdateMailRequestEmail(user, token, newEmail)

    messageEmail.sendEmail(
      senderName = Some(emailTemplate.getSenderName),
      senderEmail = emailTemplate.getSenderEmail,
      receiverName = newEmail,
      receiverEmail = newEmail,
      subject = subject,
      text = content,
      appName = emailTemplate.getAppName
    )
  }


  def sendConfirmChangedEmail(user: User, newEmail: String): Future[Expect[String]] = {
    val (subject, content) = updateEmailEmailFactory.getConfirmChangedEmail(user, newEmail)

    messageEmail.sendEmail(
      senderName = Some(emailTemplate.getSenderName),
      senderEmail = emailTemplate.getSenderEmail,
      receiverName = newEmail,
      receiverEmail = newEmail,
      subject = subject,
      text = content,
      appName = emailTemplate.getAppName
    )
  }


}
