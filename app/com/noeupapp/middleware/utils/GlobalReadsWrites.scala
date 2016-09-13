package com.noeupapp.middleware.utils

import java.sql.{PreparedStatement, Types}
import java.util.UUID

import anorm.{Column, MetaDataItem, ToStatement, TypeDoesNotMatch}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.data.validation.ValidationError
import play.api.libs.json._

import scala.util.Try


trait GlobalReadsWrites {

  /**
    * Parses JsValue to PG object
    * transform a json into a string and inject it into the SQL statement
    */
  implicit object jsonToStatement extends ToStatement[JsValue] {
    def set(s: PreparedStatement, i: Int, json: JsValue): Unit = {
      val jsonObject = new org.postgresql.util.PGobject()
      jsonObject.setType("json")
      jsonObject.setValue(Json.stringify(json))
      s.setObject(i, jsonObject)
    }
  }

  /**
    * transform a PGobject into JsValue
    */
  implicit val columnToJsValue: Column[JsValue] = anorm.Column.nonNull1[JsValue] { (value, meta) =>
    val MetaDataItem(qualified, _, _) = meta
    value match {
      case json: org.postgresql.util.PGobject => Right(Json.parse(json.getValue))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Json for column $qualified"))
    }
  }


  // Datetime
  val dateFormatGeneration: DateTimeFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmssSS")

  implicit def rowToDateTime: Column[DateTime] = Column.nonNull1 { (value, meta) =>
    value match {
      case ts: java.sql.Timestamp => Right(new DateTime(ts.getTime))
      case d: java.sql.Date => Right(new DateTime(d.getTime))
      case str: java.lang.String => Right(dateFormatGeneration.parseDateTime(str))
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass))
    }
  }

  implicit val dateTimeToStatement = new ToStatement[DateTime] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: DateTime): Unit = {
      s.setTimestamp(index, new java.sql.Timestamp(aValue.getMillis))
    }
  }

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)






  implicit def rowToOptionDateTime: Column[Option[DateTime]] = Column.nonNull1 { (value, meta) =>
    value match {
      case ts: java.sql.Timestamp => Right(Some(new DateTime(ts.getTime)))
      case d: java.sql.Date => Right(Some(new DateTime(d.getTime)))
      case str: java.lang.String => Right(Some(dateFormatGeneration.parseDateTime(str)))
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass))
    }
  }

  implicit val optionDateTimeToStatement = new ToStatement[Option[DateTime]] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: Option[DateTime]): Unit = {
      aValue match {
        case Some(value) => s.setTimestamp(index, new java.sql.Timestamp(value.getMillis))
        case None        => s.setNull(index, Types.TIMESTAMP)
      }
    }
  }




  // UUID
  implicit def rowToUUID: Column[UUID] = Column.nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, _, _) = meta
    value match {
      case d: UUID => Right(d)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":"
        + value.asInstanceOf[AnyRef].getClass + " to UUID for column " +
        qualified))
    }
  }

  implicit val uuidToStatement = new ToStatement[UUID] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: UUID):
    Unit = s.setObject(index, aValue)
  }

  /**
    * Deserializer for java.util.UUID, from latest play Reads (was added on 2014-03-01 to master,
    * see https://github.com/playframework/playframework/pull/2428)
    */
  def uuidReader(checkUuuidValidity: Boolean = false): Reads[java.util.UUID] = new Reads[java.util.UUID] {

    def check(s: String)(u: UUID): Boolean = u != null && s == u.toString

    def parseUuid(s: String): Option[UUID] = {
      val uncheckedUuid = Try(UUID.fromString(s)).toOption

      if (checkUuuidValidity) {
        uncheckedUuid filter check(s)
      } else {
        uncheckedUuid
      }
    }

    def reads(json: JsValue) = json match {
      case JsString(s) =>
        parseUuid(s).map(JsSuccess(_)).getOrElse(JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.uuid")))))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.uuid"))))
    }

  }

  implicit val uuidReads: Reads[UUID] = uuidReader()
  implicit val uuidWrites: Writes[UUID] = Writes { uuid => JsString(uuid.toString) }

}
