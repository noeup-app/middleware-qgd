package com.noeupapp.middleware.crudauto


import slick.driver.PostgresDriver.api._

import scala.language.existentials
import scala.reflect.ClassTag

trait CrudClassName {

  def configure(modelName: String): Option[CrudConfiguration[_,_,_]]


  // TODO : why cast is necessary here ?
  protected def configuration[E, PK, V <: Table[E]]()(implicit eClass: ClassTag[E], pkClass: ClassTag[PK], vClass: ClassTag[V], baseColumnType: BaseColumnType[PK]) =
    Some(CrudConfiguration[E, PK, V](eClass.runtimeClass.asInstanceOf[Class[E]],
      pkClass.runtimeClass.asInstanceOf[Class[PK]],
      vClass.runtimeClass.asInstanceOf[Class[V]]))
}

case class CrudConfiguration[E, PK, V <: Table[E]](entityClass: Class[E], pK: Class[PK], tableDef: Class[V])(implicit val baseColumnType: BaseColumnType[PK])

// TODO : CrudConfiguration fails to compile. CrudConfiguration is needed to validate types
// Ugly trick to make it work
case class CrudConfigurationUnTyped(entityClass: Class[_], pK: Class[_], tableDef: Class[_], baseColumnType: BaseColumnType[_])