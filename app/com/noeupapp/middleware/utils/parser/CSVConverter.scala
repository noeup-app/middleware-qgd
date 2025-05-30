package com.noeupapp.middleware.utils.parser

import java.io.File

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import org.joda.time.DateTime
import play.api.libs.iteratee.{Enumeratee, _}

import scala.util.{Failure, Success, Try}
import shapeless._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{-\/, \/-}
import com.noeupapp.middleware.utils.parser.CSVParseOutput._
import com.noeupapp.middleware.utils.streams.EnumerateeAdditionalOperators._
import com.noeupapp.middleware.utils.streams.EnumeratorAdditionalOperators._
import com.noeupapp.middleware.utils.streams.IterateeAdditionalOperators._
import com.noeupapp.middleware.utils.StringUtils._
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect._
import scalaz._
import Scalaz._



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


  def csvLineConverter[A](line: Line)(implicit ec: CSVConverter[A]): Try[A] =
    ec.from(line.value, Some(line))


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
        s.trim.span(_ != ';') match {
          case (before,after) =>
            for {
              front <- scv.value.from(before, context)
              back <- sct.value.from(if (after.isEmpty) after else after.tail, context)
            } yield front :: back

          case error => fail(s"Cannot convert `$s` to HList (split returned : `$error`)", context)
        }

      def to(ft: V :: T): String = {
        scv.value.to(ft.head) ++ ";" ++ sct.value.to(ft.tail)
      }
    }


  // Anything with a Generic

  implicit def deriveClass[A,R](implicit gen: Generic.Aux[A,R], conv: CSVConverter[R])
  : CSVConverter[A] = new CSVConverter[A] {

    def from(s: String, context: Option[Line] = Option.empty): Try[A] = conv.from(s, context).map(gen.from)
    def to(a: A): String = conv.to(gen.to(a))
  }


  private val convertToLine: Enumeratee[String, Line] =
    Enumeratee.zipWithIndex ><> Enumeratee.map{
      case (e, idx) => Line(idx, e)
    }

  def readHugeFile[T: ClassTag](file: File, dropFirst: Boolean = false, colOrder: List[Option[Int]] = List.empty)(implicit st: Lazy[CSVConverter[T]]): Future[Expect[CSVParseOutput[T]]] = {

    Logger.info("Parsing CSV file... " + classTag[T])


    val reOrder: Enumeratee[String, String] = Enumeratee.map{ line =>
      if(colOrder.isEmpty){
        line
      } else {
        val splitLine = splitAndKeepEmpty(line, ';')
        colOrder.map {
          case Some(idx) => splitLine.lift(idx).getOrElse("")
          case None => ""
        }.mkString(";")
      }
    }

    val parse: Enumeratee[Line, CSVParseOutput[T]] =
      Enumeratee.map{ line =>
        val res = csvLineConverter(line)(st.value)
        fromTry(line, res)
      }

    val getResult: Iteratee[CSVParseOutput[T], CSVParseOutput[T]] =
      Iteratee.monadicFold[CSVParseOutput[T]]

    (Enumerator.fromUTF8File(file) &>
      Enumeratee.splitToLines ><>
        Enumeratee.drop(dropFirst.option(1).getOrElse(0)) ><> // Drop first line
        reOrder ><>
        convertToLine ><>
        parse |>>
      getResult).flatMap(_.run.map{ r =>
      Logger.info("Parsed CSV file " + classTag[T])

      \/-(r)
    })
      .recover {
        case t: Exception =>
          Logger.error(t.getMessage)
          -\/(FailError(t))
      }
  }

  def readHugeFileWithoutTypeCheck(file: File, from: Int, to: Option[Int] = None): Future[Expect[List[CustomTypedLine[Map[String, String]]]]] = {


    val alignFileContent: Enumeratee[String, String] = Enumeratee.map{ line =>
      if(from == 0 && to.isEmpty){
        line
      } else {
        val splitLine = splitAndKeepEmpty(line, ';')
        to match {
          case Some(i) => splitLine.splitAt(from)._2.splitAt(i)._1.mkString(";") // not clever to split, join and then to split again in the flow
          case None => splitLine.splitAt(from)._2.mkString(";") // not clever to split, join and then to split again in the flow
        }
      }
    }

    val splitLines: Enumeratee[String, List[String]] = Enumeratee.map(splitAndKeepEmpty(_, ';'))

    val convertToLine: Enumeratee[List[String], CustomTypedLine[List[String]]] =
      Enumeratee.zipWithIndex ><> Enumeratee.map{
        case (e, idx) => CustomTypedLine(idx, e)
      }


    val firstLineFuture: Future[Option[List[String]]] =
      (Enumerator.fromUTF8File(file) &>
       Enumeratee.splitToLines ><>
       alignFileContent ><>
       splitLines |>>
       Iteratee.head).flatMap(_.run)


    val fileReadFuture: Future[List[CustomTypedLine[List[String]]]] =
      (Enumerator.fromUTF8File(file) &>
        Enumeratee.splitToLines ><>
        Enumeratee.drop(1) ><> // Drop first line
        alignFileContent ><>
        splitLines ><>
        convertToLine |>>
        Iteratee.getChunks).flatMap(_.run)


    {
      for {
        firstLineOpt <- firstLineFuture
        fileRead     <- fileReadFuture
      } yield {
        firstLineOpt match {
          case None => -\/(FailError("File is empty"))
          case Some(firstLine) =>
            \/-{
              fileRead.map { line =>
                CustomTypedLine(line.number, firstLine.zip(line.value).toMap)
              }
            }
        }
      }
    }.recover {
      case t: Exception =>
        Logger.error(t.getMessage)
        -\/(FailError(t))
    }

  }

}

