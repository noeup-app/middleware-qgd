package com.noeupapp.middleware.utils.parser

import play.api.libs.json.Json


case class Line(number: Int, value: String)

case class CustomTypedLine[T](number: Int, value: T)


object Line {
  implicit val lineFormat = Json.format[Line]
}