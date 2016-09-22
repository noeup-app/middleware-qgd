package com.noeupapp.middleware.utils.parser

import scala.util.{Failure, Success, Try}

import shapeless._, syntax.singleton._

import scala.collection.immutable.{:: => Cons}

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

  implicit def stringOptCSVConverter: CSVConverter[Option[String]] = new CSVConverter[Option[String]] {
    def from(s: String, context: Option[Line] = Option.empty): Try[Option[String]] =
      s.trim match {
        case str if str.isEmpty => Success(None)
        case str => Success(Some(str))
      }
    def to(s: Option[String]): String = s.getOrElse("")
  }

  implicit def intCsvConverter: CSVConverter[Int] = new CSVConverter[Int] {
    def from(s: String, context: Option[Line] = Option.empty): Try[Int] = Try(s.trim.toInt)
    def to(i: Int): String = i.toString
  }

  def listCsvLinesConverter[A](l: List[Line])(implicit ec: CSVConverter[A])
  : Try[List[A]] = l match {
    case Nil => Success(Nil)
    case Cons(s,ss) => for {
      x <- ec.from(s.value, Some(s))
      xs <- listCsvLinesConverter(ss)(ec)
    } yield Cons(x, xs)
  }

  implicit def listCsvConverter[A](implicit ec: CSVConverter[A])
  : CSVConverter[List[A]] = new CSVConverter[List[A]] {
    def from(s: String, context: Option[Line] = Option.empty): Try[List[A]] = {
      val lines: List[Line] =
        s
          .trim
          .split("\n").toList
          .zipWithIndex
          .map{
            case (content, lineNumber) => Line(lineNumber, content)
          }
      listCsvLinesConverter(lines)(ec)
    }
    def to(l: List[A]): String = l.map(ec.to).mkString("\n")
  }


  // HList

  implicit def deriveHNil: CSVConverter[HNil] =
    new CSVConverter[HNil] {
      def from(s: String, context: Option[Line] = Option.empty): Try[HNil] =
        s match {
          case "" => Success(HNil)
          case s => fail("Cannot convert '" ++ s ++ "' to HNil", context)
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

          case _ => fail("Cannot convert '" ++ s ++ "' to HList", context)
        }

      def to(ft: V :: T): String = {
        scv.value.to(ft.head) ++ "," ++ sct.value.to(ft.tail)
      }
    }

  implicit def deriveHConsOption[V, T <: HList]
  (implicit scv: Lazy[CSVConverter[V]], scvOpt: Lazy[CSVConverter[Option[V]]], sct: Lazy[CSVConverter[T]])
  : CSVConverter[Option[V] :: T] =
    new CSVConverter[Option[V] :: T] {

      def from(s: String, context: Option[Line] = Option.empty): Try[Option[V] :: T] =
        s.trim.span(_ != ',') match {
          case (before,after) =>
            (for {
              front <- scvOpt.value.from(before, context)
              back <- sct.value.from(if (after.isEmpty) after else after.tail, context)
            } yield front :: back).orElse {
              sct.value.from(s.trim, context).map(None :: _)
            }

          case _ => fail("Cannot convert '" ++ s ++ "' to HList", context)
        }

      def to(ft: Option[V] :: T): String = {
        ft.head.map(scv.value.to(_) ++ ",").getOrElse("") ++ sct.value.to(ft.tail)
      }
    }


  // Anything with a Generic

  implicit def deriveClass[A,R](implicit gen: Generic.Aux[A,R], conv: CSVConverter[R])
  : CSVConverter[A] = new CSVConverter[A] {

    def from(s: String, context: Option[Line] = Option.empty): Try[A] = conv.from(s, context).map(gen.from)
    def to(a: A): String = conv.to(gen.to(a))
  }
}

