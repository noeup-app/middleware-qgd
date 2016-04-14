package com.noeupapp.middleware.utils

import anorm.{TypeDoesNotMatch, MetaDataItem, Column, ToStatement}
import org.postgresql.util.PGobject
//import org.postgresql.jdbc4.Jdbc4Array
import play.api.libs.json._
import scala.language.implicitConversions

/**
  * // TODO comment
  *
  * http://stackoverflow.com/questions/15488639/how-to-write-readst-and-writest-in-scala-enumeration-play-framework-2-1
  */
object EnumUtils {

  // TODO add comments ?

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }




  def toStatement[A](vToString: (A => String), transformer: (String => String) = toSnakeCase _): ToStatement[A] = new ToStatement[A] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: A): Unit = s.setObject(index, transformer(vToString(aValue)))
  }

  /**
    * Only used if grant type is also an ENUM in PG
    *
    * See http://bwbecker.github.io/blog/2015/05/05/postgres-enums-and-anorm/ to fix
    */
//  def fromColumn[A](parser: String => Option[A], transformer: String => String = toCamelCase _): Column[A] = Column.nonNull1 { (value, meta) =>
//    val MetaDataItem(qualified, nullable, clazz) = meta
//    val error = TypeDoesNotMatch("fromColumn : Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " for column " + qualified)
//    value match {
//      case s: PGobject => {
//        parser(transformer(s.getValue)).toRight(error)
//      }
//      case _ => {
//        parser(transformer(value.toString)).toRight(error)
//      }
//    }
//  }

  def toSnakeCase(name: String) = {
    val words: List[String] = name.foldLeft(List[String]()){ (words, char) =>
      if(char.isUpper) {
        char.toLower.toString :: words
      } else {
        words match {
          case h :: t => (h + char.toLower.toString) :: t
          case _ => List(char.toLower.toString)
        }
      }
    }
    words.reverse.mkString("-")
  }

  def toCamelCase(name: String) = name.split("-").toList.map(_.capitalize).mkString

}
