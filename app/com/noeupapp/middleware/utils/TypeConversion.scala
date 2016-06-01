package com.noeupapp.middleware.utils

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._

import scalaz.{-\/, \/-}

object TypeConversion {

  def expectOption2Expect[T](opt: Expect[Option[T]]): Expect[T] = {
    opt match {
      case \/-(Some(r)) => \/-(r)
      case \/-(None) => -\/(FailError("None"))
      case -\/(_) => -\/(FailError("request failed"))
    }
  }

  def option2Expect[T](opt: Option[T]): Expect[T] = {
    opt match {
      case Some(r) => \/-(r)
      case None => -\/(FailError("None"))
    }
  }

}