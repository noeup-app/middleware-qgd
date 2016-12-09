package com.noeupapp.middleware.utils.parser

import java.io.File

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import org.joda.time.DateTime
import play.api.libs.iteratee.Enumeratee.{CheckDone, Grouped}
import play.api.libs.iteratee.Execution.Implicits._
import play.api.libs.iteratee.{Done, Input, _}

import scala.util.{Failure, Success, Try}
import shapeless._
import syntax.singleton._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.{:: => Cons}
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, Monoid, \/-}
import com.noeupapp.middleware.utils.parser.CSVConverter._
import play.api.libs.json.Json

// Implementation

/** Exception to throw if something goes wrong during CSV parsing */
case class CSVException(s: String, line: Option[Line]) extends RuntimeException(s) {
  override def toString: String = s"CSVException(cause = $s, line = $line)"
}

case class Line(number: Int, value: String)

object Line {
  implicit val lineFormat = Json.format[Line]
}

case class CSVParseOutput[T](failures: List[(Line, FailError)], successes: List[(Line, T)])

object CSVParseOutput {

  def empty[T]: CSVParseOutput[T] = CSVParseOutput[T](List.empty, List.empty)

  implicit def cSVParseOutputMonoid[F] = new Monoid[CSVParseOutput[F]] {

    override def zero: CSVParseOutput[F] = CSVParseOutput(List.empty, List.empty)

    override def append(f1: CSVParseOutput[F], f2: => CSVParseOutput[F]): CSVParseOutput[F] =
      CSVParseOutput(
        f1.failures ++ f2.failures,
        f1.successes ++ f2.successes
      )

  }
}


/** Trait for types that can be serialized to/deserialized from CSV */
trait CSVConverter[T] {
  def from(s: String, context: Option[Line] = Option.empty): Try[T]
  def to(t: T): String
}

/** Instances of the CSVConverter trait */
object CSVConverter {
  def apply[T](implicit st: Lazy[CSVConverter[T]]): CSVConverter[T] = st.value

  def fail(s: String, line: Option[Line]) = Failure(CSVException(s, line))


  // Primitives

  implicit def stringCSVConverter: CSVConverter[String] = new CSVConverter[String] {
    def from(s: String, context: Option[Line] = Option.empty): Try[String] =
      s.trim match {
        case str if str.isEmpty => fail("Missing required value", context)
        case str => Success(str)
      }
    def to(s: String): String = s
  }

  implicit def optCSVConverter[T](implicit st: Lazy[CSVConverter[T]]): CSVConverter[Option[T]] = new CSVConverter[Option[T]] {
    def from(s: String, context: Option[Line] = Option.empty): Try[Option[T]] =
      s.trim match {
        case str if str.isEmpty => Success(None)
        case str =>
          for{
            v <- st.value.from(str)
          } yield Some(v)
      }
    def to(s: Option[T]): String = s.map(st.value.to).getOrElse("")

  }

  implicit def intCsvConverter: CSVConverter[Int] = new CSVConverter[Int] {
    def from(s: String, context: Option[Line] = Option.empty): Try[Int] =
      Try(s.trim.toInt) match {
        case Failure(e: Exception) => Failure(new Exception(s"${e.getMessage} ; context : $context"))
        case e => e
    }

    def to(i: Int): String = i.toString
  }

  implicit def longCsvConverter: CSVConverter[Long] = new CSVConverter[Long] {
    def from(s: String, context: Option[Line] = Option.empty): Try[Long] =
      Try(s.trim.toLong) match {
        case Failure(e: Exception) => Failure(new Exception(s"${e.getMessage} ; context : $context"))
        case e => e
    }

    def to(i: Long): String = i.toString
  }

  implicit def booleanCsvConverter: CSVConverter[Boolean] = new CSVConverter[Boolean] {
    def from(s: String, context: Option[Line] = Option.empty): Try[Boolean] = Try(s.trim.toBoolean)

    def to(i: Boolean): String = i.toString
  }


  implicit def doubleCsvConverter: CSVConverter[Double] = new CSVConverter[Double] {
    def from(s: String, context: Option[Line] = Option.empty): Try[Double] = Try(s.trim.toDouble)
    def to(i: Double): String = i.toString
  }

  implicit def dateTimeCsvConverter: CSVConverter[DateTime] = new CSVConverter[DateTime] {
    def from(s: String, context: Option[Line] = Option.empty): Try[DateTime] = Try(DateTime.parse(s.trim))
    def to(i: DateTime): String = i.toString
  }

  def listCsvLinesConverter[A](l: List[Line])(implicit ec: CSVConverter[A])
  : CSVParseOutput[A] = {
    l.map(l => (l, ec.from(l.value, Some(l))))
      .partition(_._2.isFailure) match {
        case (fas, sus) =>
          CSVParseOutput[A](
            fas.map(s => (s._1, FailError(s._2.failed.get))),
            sus.map(s => (s._1, s._2.get))
          )
      }
  }


  def stringToListOfLines(s: String): List[Line] = {
    s
      .trim
      .split("\n").toList
      .zipWithIndex
      .map {
        case (content, lineNumber) => Line(lineNumber, content)
      }
  }

  // HList


  implicit def deriveHNil: CSVConverter[HNil] =
    new CSVConverter[HNil] {
      def from(s: String, context: Option[Line] = Option.empty): Try[HNil] =
        s match {
          case "" => Success(HNil)
          case found => fail(s"Cannot convert `$s` to HNil (found : `$found`)", context)
        }

      def to(n: HNil) = ""
    }

  implicit def deriveHCons[V, T <: HList]
  (implicit scv: Lazy[CSVConverter[V]], sct: Lazy[CSVConverter[T]])
  : CSVConverter[V :: T] =
    new CSVConverter[V :: T] {

      def from(s: String, context: Option[Line] = Option.empty): Try[V :: T] =
        s.trim.span(_ != ',') match {
          case (before,after) =>
            for {
              front <- scv.value.from(before, context)
              back <- sct.value.from(if (after.isEmpty) after else after.tail, context)
            } yield front :: back

          case error => fail(s"Cannot convert `$s` to HList (split returned : `$error`)", context)
        }

      def to(ft: V :: T): String = {
        scv.value.to(ft.head) ++ "," ++ sct.value.to(ft.tail)
      }
    }


  // Anything with a Generic

  implicit def deriveClass[A,R](implicit gen: Generic.Aux[A,R], conv: CSVConverter[R])
  : CSVConverter[A] = new CSVConverter[A] {

    def from(s: String, context: Option[Line] = Option.empty): Try[A] = conv.from(s, context).map(gen.from)
    def to(a: A): String = conv.to(gen.to(a))
  }


  def readHugeFile[T](file: File, cSVAction: Option[CSVAction] = None)(implicit st: Lazy[CSVConverter[T]]): Future[Expect[CSVParseOutput[T]]] = {


    val chunkSize = 1024 * 8

    val enumerator: Enumerator[Array[Byte]] = Enumerator.fromFile(file, chunkSize)

    def isLastChunk(chunk: Array[Byte]): Boolean = {
      chunk.length < chunkSize
    }


    val groupByLines: Enumeratee[Array[Byte], List[String]] = Enumeratee.grouped {
      println("groupByLines")
      Iteratee.fold[Array[Byte], (String, List[String])]("", List.empty) {
        case ((accLast, accLines), chunk) =>
          println("groupByLines chunk size " + chunk.length)
          new String(chunk)
            .trim
            .split("\n")
            .toList match {
            case lines  @ Cons(h, tail) =>
              val lineBetween2Chunks: String = accLast + h

              val goodLines =
                isLastChunk(chunk) match {
                  case true  => Cons(lineBetween2Chunks, tail)
                  case false => Cons(lineBetween2Chunks, tail).init
                }

              (lines.last, accLines ++ goodLines)
            case Nil => ("", accLines)
          }
      }.map(_._2)
    }


    val turnIntoLines: Enumeratee[List[String], List[Line]] = Enumeratee.grouped {
      println("turnIntoLines")
      Iteratee.fold[List[String], (Int, List[Line])](0, List.empty) {
        case ((index, accLines), chunk) =>
          println("turnIntoLines chunk size " + chunk.length)
          val lines =
            ((Stream from index) zip chunk).map {
              case (lineNumber, content) => Line(lineNumber, content)
            }.toList
          (index + chunk.length, lines ++ accLines)
      }.map(_._2)
    }


    val parseChunk: Iteratee[List[Line], CSVParseOutput[T]] = {
      println("parseChunk")
      Iteratee.fold[List[Line], CSVParseOutput[T]](CSVParseOutput.empty) {
        case (_, chunk) if cSVAction.isDefined =>
          println(("chunk", chunk.size))
          //          chunk.foreach(l => println((l.number, l.value.filter(_ == ',').length, l.value)))
          listCsvLinesConverter[T](CSVSpitter.splitCSV(chunk, cSVAction.get))(st.value)
        case (_, chunk) if cSVAction.isEmpty =>
          println(("chunk", chunk.size))
//          chunk.foreach(l => println((l.number, l.value.filter(_ == ',').length, l.value)))
          listCsvLinesConverter[T](chunk)(st.value)
      }
    }


      (enumerator &> groupByLines ><> turnIntoLines  |>> parseChunk).flatMap(_.run).map(\/-(_))
        .recover{
          case e: Exception => -\/(FailError(e))
        }
  }

}

