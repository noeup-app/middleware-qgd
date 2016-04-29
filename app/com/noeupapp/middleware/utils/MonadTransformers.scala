package com.noeupapp.middleware.utils

import com.noeupapp.middleware.errorHandle.FailError._
import play.api.Logger

import scala.annotation.tailrec
import scalaz.{-\/, \/-}

object MonadTransformers {
  @tailrec
  final def expectList2ListExpect[T](elements: List[Expect[T]], buffer: List[T] = List.empty): Expect[List[T]] = elements match {
    case element :: tail  =>
      Logger.trace(s"MonadTransformers.expectList2ListExpect($elements, $buffer)")

      element match {
        case \/-(res) =>
          Logger.trace(s"MonadTransformers.expectList2ListExpect($elements, $buffer) -> $res")
          expectList2ListExpect(tail, res :: buffer)
        case -\/(error) => -\/(error)
      }
    case Nil                =>
      \/-(buffer)
  }
}
