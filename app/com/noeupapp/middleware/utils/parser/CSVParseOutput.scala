package com.noeupapp.middleware.utils.parser

import com.noeupapp.middleware.errorHandle.FailError

import scala.util.{Failure, Success, Try}
import scalaz.Monoid

case class CSVParseOutput[T](failures: List[(Line, FailError)], successes: List[(Line, T)])

object CSVParseOutput {

  def empty[T]: CSVParseOutput[T] = CSVParseOutput[T](List.empty, List.empty)

  def fromTry[T](line: Line, tryIn: Try[T]): CSVParseOutput[T] = tryIn match {
    case Success(value) => CSVParseOutput(List.empty, List((line, value)))
    case Failure(error) => CSVParseOutput(List((line, FailError(error))), List.empty)
  }

  implicit def cSVParseOutputMonoid[F] = new Monoid[CSVParseOutput[F]] {

    override def zero: CSVParseOutput[F] = CSVParseOutput(List.empty, List.empty)

    override def append(f1: CSVParseOutput[F], f2: => CSVParseOutput[F]): CSVParseOutput[F] =
      CSVParseOutput(
        f1.failures ++ f2.failures,
        f1.successes ++ f2.successes
      )

  }
}