package qgd.middleware.utils

import play.api.libs.json.{Json, Reads, Writes}

import scala.language.implicitConversions

trait CaseClassUtils {
  implicit def caseClassToString[T](o: T)(implicit writes: Writes[T]): String =
    Json.stringify(Json.toJson(o))

  implicit def stringToCaseClass[T](input: String)(implicit reads: Reads[T]): Option[T] =
    Json.fromJson(Json.parse(input)).asOpt
}
