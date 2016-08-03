package com.noeupapp.middleware.crudauto


import play.api.libs.json.Json

import scala.language.{implicitConversions, postfixOps}

case class CrudAuto(
                   className: String,
                   classAttributes: Array[String],
                   tableName: String
                   )

object CrudAuto {

  implicit val crudAutoFormat = Json.format[CrudAuto]

  val supportedClasses = Array("Entity")

}