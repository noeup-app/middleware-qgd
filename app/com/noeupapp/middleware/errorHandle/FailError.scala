package com.noeupapp.middleware.errorHandle

import play.api.mvc.Result
import play.api.mvc.Results._

import scala.concurrent.Future
import scalaz._

/**
  *
  * @param message
  * @param cause
  * @param errorType
  * @param prevError
  * @param clientReadableErrorMessage represents a message that could be returned to client
  *                                   MUST NOT CONTAIN ANY INFORMATION ABOUT THE APP !
  */
final case class FailError(message: String,
                           cause: Option[\/[Throwable, Error]] = None,
                           errorType: Status = InternalServerError,
                           prevError: Option[FailError] = None,
                           clientReadableErrorMessage: Option[String] = None) {

  def getException = {
    cause match {
      case Some(-\/(e)) => throw e
      case Some(\/-(e)) => throw e
      case _ => throw new FailErrorException(message)
    }
  }

  val fullStackTrace = Thread.currentThread().getStackTrace

  val origin: Option[String] = ExceptionEither.TryToOption(fullStackTrace.toList.mkString("\n"))

  notifyUs() // TODO use akka actors to send notification if plugin available

  def notifyUs(): Unit ={
//    val subject = "Server error"
//    val content = s"An error occurred : " +
//      s"\n\n\n" +
//      s"Message : $message" +
//      s"\n\n" +
//      s"Cause : $cause" +
//      s"\n\n" +
//      s"Guessed origin : $origin" +
//      s"\n\n" +
//      s"Full StackTrace : ${fullStackTrace.toList}"
//    val to = List("Support Cowebo <support@cowebo.com>")
//    val from = "Support Cowebo <support@cowebo.com>"
//    val mail = Email(subject, from, to, attachments = Seq(), bodyText = None, bodyHtml = Some(content))
    /*FTry(MailerPlugin.send(mail)).map(_.map(r => Json.parse("{\"messageId\": \"" + r + "\"}")))*/ // TODO uncomment and use injection and actor
  }

  def toResult: Result = errorType(clientReadableErrorMessage.getOrElse("En error occurred!"))

  override def toString: String = s"Error($message, ${
    cause.map{
      case -\/(exception) =>
        "Exception message : " + exception.getMessage + "\n" +
          exception.getStackTrace.toList.mkString("\n")
      case \/-(fail) => fail.toString
      //    cause
    }}, $origin)" ++ prevError.map(e => s"\n Previous Error : $e").getOrElse("")
}

class FailErrorException(msg: String) extends Exception(msg)

object FailError {

  type Expect[T] = \/[FailError, T]


  implicit val failErrorMonoid = new Monoid[FailError] {
    override def zero: FailError = FailError("no error")

    override def append(f1: FailError, f2: => FailError): FailError =
      f2.copy(prevError = Some(f1))
  }

  // Alternative constructor to simplify error creation


  def apply(message: String, cause: Throwable, errorType: Status): FailError = FailError(message, Some(-\/(cause)), errorType)

  def apply(message: String, cause: Throwable): FailError = FailError(message, Some(-\/(cause)))

  def apply(exception: Throwable): FailError = FailError(exception.getMessage, exception)

}

object Expect {

  def right[T](block: T): FailError.Expect[T] = \/-(block)
  def futureRight[T](block: T): Future[FailError.Expect[T]] = Future.successful(\/-(block))

}