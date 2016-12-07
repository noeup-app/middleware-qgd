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
import scalaz.{-\/, \/-}

// Implementation

/** Exception to throw if something goes wrong during CSV parsing */
case class CSVException(s: String, line: Option[Line]) extends RuntimeException(s) {
  override def toString: String = s"CSVException(cause = $s, line = $line)"
}

case class Line(number: Int, value: String)

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
  : Try[List[A]] = l match {
    case Nil => Success(Nil)
    case Cons(s,ss) =>
      for {
        x <- ec.from(s.value, Some(s))
        xs <- listCsvLinesConverter(ss)(ec)
      } yield Cons(x, xs)
  }

  implicit def listCsvConverter[A](implicit ec: CSVConverter[A])
  : CSVConverter[List[A]] = new CSVConverter[List[A]] {
    def from(s: String, context: Option[Line] = Option.empty): Try[List[A]] = {
      val lines: List[Line] = stringToListOfLines(s)
      listCsvLinesConverter(lines)(ec)
    }
    def to(l: List[A]): String = l.map(ec.to).mkString("\n")
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


  def readHugeFile[T](file: File, cSVAction: Option[CSVAction] = None)(implicit st: Lazy[CSVConverter[T]]): Future[Expect[List[T]]] = {


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


//    def test: Enumeratee[List[Line],List[Line]] = new Enumeratee[List[Line],List[Line]]{
//      override def applyOn[A](inner: Iteratee[List[Line], A]): Iteratee[List[Line], Iteratee[List[Line], A]] = new CheckDone[List[Line], List[Line]] {
//
//        def step[B](f: Iteratee[List[Line], List[Line]]): K[List[Line], Iteratee[List[Line], B]] = {
//
//          case in @ (Input.El(_) | Input.Empty) =>
//
//            Error("test", in)
//          case Input.EOF => Done(List.empty)
//
//        }
//
//        def continue[B](k: K[List[Line], A]) = Cont(step(k))
//      }
//    }

//
//    def grouped[From] = new Grouped[From] {
//
//      def apply[To](folder: Iteratee[From, To]): Enumeratee[From, To] = new CheckDone[From, To] {
//
//        def step[A](f: Iteratee[From, To])(k: K[To, A]): K[From, Iteratee[To, A]] = {
//
//          case in @ (Input.El(_) | Input.Empty) =>
//
//            Iteratee.flatten(f.feed(in)).pureFlatFold {
//              case Step.Done(a, left) => new CheckDone[From, To] {
//                def continue[A](k: K[To, A]) =
//                  (left match {
//                    case Input.El(_) => step(folder)(k)(left)
//                    case _ => Cont(step(folder)(k))
//                  })
//              } &> k(Input.El(a))
//              case Step.Cont(kF) => Cont(step(Cont(kF))(k))
//              case Step.Error(msg, e) => Error(msg, in)
//            }(dec)
//
//          case Input.EOF => Iteratee.flatten(f.run.map[Iteratee[From, Iteratee[To, A]]]((c: To) => Done(k(Input.El(c)), Input.EOF))(dec))
//
//        }
//
//        def continue[A](k: K[To, A]) = Cont(step(folder)(k))
//      }
//    }


//
//    def groupByChunk[A](chunkSize: Int): Enumeratee[List[A], List[A]] = Enumeratee.grouped(
//      Enumeratee.splitOnceAt
//    )


    val parseChunk: Iteratee[List[Line], Try[List[T]]] = {
      println("parseChunk")
      Iteratee.fold[List[Line], Try[List[T]]](Try(List.empty)) {
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


//    val future =
      (enumerator &> groupByLines ><> turnIntoLines  |>> parseChunk).flatMap(_.run) map {
        case Success(value) => \/-(value)
        case Failure(error) => -\/(FailError(error))
      }
  }

}

