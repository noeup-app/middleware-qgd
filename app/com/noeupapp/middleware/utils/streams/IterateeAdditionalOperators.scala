package com.noeupapp.middleware.utils.streams

import java.io.File
import java.nio.charset.Charset

import play.api.libs.iteratee.{Enumerator, Input, Iteratee}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.Monoid


object IterateeAdditionalOperators {
  implicit def iterateeAdditionalOperators(e: Iteratee.type): IterateeAdditionalOperators = new IterateeAdditionalOperators(e)
}


class IterateeAdditionalOperators(e: Iteratee.type) {

  def monadicFold[T]()(implicit tM: Monoid[T]): Iteratee[T, T] =
    e.fold[T, T](tM.zero)(tM.append(_, _))

}
