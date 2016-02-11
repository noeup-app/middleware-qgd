package qgd.errorHandle

import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.mailer._
import play.api.mvc.Results._
import qgd.errorHandle.ExceptionEither._

import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._

final case class FailError(message: String, cause: Option[\/[Throwable, Error]] = None, errorType: Status = InternalServerError) {

  val fullStackTrace = Thread.currentThread().getStackTrace

  val origin: Option[String] = ExceptionEither.TryToOption(fullStackTrace.toList.mkString("\n"))

  notifyUs() // TODO use akka actors to send notification if plugin available

  def notifyUs(): Unit ={
    val subject = "Server error"
    val content = s"An error occurred : " +
      s"\n\n\n" +
      s"Message : $message" +
      s"\n\n" +
      s"Cause : $cause" +
      s"\n\n" +
      s"Guessed origin : $origin" +
      s"\n\n" +
      s"Full StackTrace : ${fullStackTrace.toList}"
    val to = List("Support Cowebo <support@cowebo.com>")
    val from = "Support Cowebo <support@cowebo.com>"
    val mail = Email(subject, from, to, attachments = Seq(), bodyText = None, bodyHtml = Some(content))
    /*FTry(MailerPlugin.send(mail)).map(_.map(r => Json.parse("{\"messageId\": \"" + r + "\"}")))*/ // TODO uncomment and use injection and actor
  }

  def toResult(message: Option[String] = None) = errorType(message.getOrElse(this.message))

  override def toString: String = s"Error($message, ${
    cause.map{
      case -\/(exception) => exception.getStackTrace.toList.mkString("\n")
      case \/-(fail) => fail.toString
      //    cause
    }}, $origin)"
}

object FailError {

  type Expect[T] = \/[FailError, T]

  // Alternative constructor to simplify error creation
  def apply(message: String, cause: Throwable): FailError = FailError(message, Some(-\/(cause)))


}