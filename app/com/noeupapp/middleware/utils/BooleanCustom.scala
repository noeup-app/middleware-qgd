package com.noeupapp.middleware.utils

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import scala.language.implicitConversions

object BooleanCustom {
  implicit def asBooleanCustom(boolean: Boolean): BooleanCustom = new BooleanCustom(boolean)
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
  def |>(message: String): Future[Expect[Unit]] = {
    if(boolean){
      Future(\/-(()))
    }else{
      Future(-\/(FailError(message)))
    }
  }
}