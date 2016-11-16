package com.noeupapp.middleware.crudauto


import slick.driver._
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

import scala.collection.immutable.HashMap
import scala.language.existentials

/**
  * Created by damien on 15/11/2016.
  */
trait CrudClassName {

//  protected def configure[U, V : TableQuery[U]](config: Config[U, V]): Config[U, V]

//  private lazy val configResult = configure(new Config).config

  def getClassNames[E <: Entity, PK: BaseColumnType, V <: PKTable[E, PK]](modelName: String): Option[CrudClassNameValue]// =
//    configure[U, V](new Config[U, V]).config.get(modelName)

}

//
//class Config[U, V : TableQuery[U]](val config : Map[String, CrudClassNameValue[U, V]] = Map.empty) {
//  def bindToken(clazz: String) = new CrudConfigBinder(config, clazz)
//}
//
//class CrudConfigBinder[U, V : TableQuery[U]](config : Map[String, CrudClassNameValue[U, V]], className: String) {
//  def to[U, V : TableQuery[U]](cons: Tag => V)(implicit c: Class[U]) =
//    new Config(config + (className -> CrudClassNameValue(c, new TableQuery[V](cons))))
//}
//

//case class CrudClassNameValue(entityClass: Class[_], pK: Class[_], tableQuery: Class[_])
case class CrudClassNameValue(entityClass: Class[_], pK: Class[_], tableQuery: Class[_])