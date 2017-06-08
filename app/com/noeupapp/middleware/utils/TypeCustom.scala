package com.noeupapp.middleware.utils

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._
import play.api.mvc.Results.Status

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import scala.language.implicitConversions
import play.api.mvc.Results._

object TypeCustom {
  implicit def asBooleanCustom(boolean: Boolean): BooleanCustom = new BooleanCustom(boolean)
  implicit def asOptionCustom[T](option: Option[T]): OptionCustom[T] = new OptionCustom(option)
  implicit def asListCustom[T](list: List[T]): ListCustom[T] = new ListCustom(list)
}

class BooleanCustom(boolean: Boolean) {


  /**
    * This is a method change a boolean into a Future[Expect].
    *   if false return message given in FailError
    *
    * How to use ?
    *   for {
    *     // ...
    *     _ <- EitherT(myBoolean |> "Error message returned if myBoolean is false")
    *     // ...
    *   } yield // ...
    *
    * @param message message returned if boolean is false
    * @return
    */
  def |>(message: => String, errorType: Status = InternalServerError): Future[Expect[Unit]] = {
    if(boolean){
      Future.successful(\/-(()))
    }else{
      Future.successful(-\/(FailError(message, errorType = errorType, clientReadableErrorMessage = Some(message))))
    }
  }
}

class OptionCustom[T](option: Option[T]) {


  /**
    * This is a method change an option into a Future[Expect].
    *   if false return message given in FailError
    *
    * How to use ?
    *   for {
    *     // ...
    *     _ <- EitherT(myOption |> "Error message returned if myOption is None")
    *     // ...
    *   } yield // ...
    *
    * @param message message returned if option is None
    * @return
    */
  def |>(message: String, errorType: Status = InternalServerError): Future[Expect[T]] = {
    option match {
      case Some(e) => Future.successful(\/-(e))
      case None    => Future.successful(-\/(FailError(message, errorType = errorType, clientReadableErrorMessage = Some(message))))
    }
  }
}

class ListCustom[T](list: List[T]) {


  /**
    * This is a method change an List into a Future[Expect].
    *   if Nil return message given in FailError
    *
    * How to use ?
    *   for {
    *     // ...
    *     _ <- EitherT(myList |> "Error message returned if myOption is None")
    *     // ...
    *   } yield // ...
    *
    * @param message message returned if List is Nil
    * @return
    */
  def |>(message: String, errorType: Status = InternalServerError): Future[Expect[List[T]]] = {
    list match {
      case (l @ (h::t)) => Future.successful(\/-(l))
      case Nil    => Future.successful(-\/(FailError(message, errorType = errorType, clientReadableErrorMessage = Some(message))))
    }
  }
}
