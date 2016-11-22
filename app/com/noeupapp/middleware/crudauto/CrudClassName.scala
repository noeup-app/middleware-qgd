package com.noeupapp.middleware.crudauto


import slick.driver._
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}

import scala.collection.immutable.HashMap
import scala.language.existentials

trait CrudClassName {

  def getClassNames(modelName: String): Option[CrudClassNameValue[_,_,_]]

}

case class CrudClassNameValue[E, PK, V <: Table[E]](entityClass: Class[E], pK: Class[PK], tableDef: Class[V])(implicit val baseColumnType: BaseColumnType[PK])

// TODO : CrudClassNameValue fails to compile. CrudClassNameValue is needed to validate types
// Ugly trick to make it work
case class CrudClassNameValueUnTyped(entityClass: Class[_], pK: Class[_], tableDef: Class[_], baseColumnType: BaseColumnType[_])
