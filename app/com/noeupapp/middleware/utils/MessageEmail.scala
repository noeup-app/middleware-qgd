package com.noeupapp.middleware.utils

import play.api.Play.current

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import java.io.File

import scala.util.{Failure, Success, Try}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import views.html._
import play.api.libs.mailer._
import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._
import org.apache.commons.mail.EmailAttachment
import com.noeupapp.middleware.views.html.emailTemplate

import scala.concurrent.Future
import scalaz._

class MessageEmail @Inject()(mailerClient: MailerClient){

  /**
    * Send an email using MailerClient
    *
    * @param senderName
    * @param senderEmail
    * @param receiverName
    * @param receiverEmail
    * @param subject
    * @param text
    * @param appName
    * @param bcc
    * @return
    */
  def sendEmail(senderName: Option[String], senderEmail: String, receiverName: String, receiverEmail: String,
                 subject: String, text: String, appName: String, bcc: List[String] = Nil): Future[Expect[String]] = {

    Try {
      //IDEA shows an error but still compiles
      val htmlTemplate = emailTemplate(subject, text, receiverName, senderName, appName)
      val email = Email(
        subject,
        from = senderName.map(name => s"$name <$senderEmail>").getOrElse(senderEmail),
        to = Seq(s"$receiverName <$receiverEmail>"),
        bcc = bcc,
        bodyHtml = Some(s"$htmlTemplate")
      )
      mailerClient.send(email)
    } match {
      case Failure(e) => Future.successful(-\/(FailError("Error while sending Email", e)))
      case Success(res) => Future.successful(\/-(res))
    }
  }
}