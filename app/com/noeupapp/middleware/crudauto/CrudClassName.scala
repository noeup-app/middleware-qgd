package com.noeupapp.middleware.crudauto

import slick.ast.NumericTypedType
import slick.driver._
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.api.longColumnType
import slick.lifted.{Isomorphism, TableQuery, Tag}

import scala.collection.immutable.HashMap
import scala.language.existentials
import scala.reflect.ClassTag

trait CrudClassName {

  // TODO this should be imported from ImplicitColumnTypes (accepted with Damien :-) )
//  implicit def isomorphicType[A, B](implicit iso: Isomorphism[A, B], ct: ClassTag[A], jt: BaseColumnType[B]): BaseColumnType[A] =
//    MappedColumnType.base[A, B](iso.map, iso.comap)
//  implicit def booleanColumnType: BaseColumnType[Boolean]
//  implicit def bigDecimalColumnType: BaseColumnType[BigDecimal] with NumericTypedType
//  implicit def byteColumnType: BaseColumnType[Byte] with NumericTypedType
//  implicit def charColumnType: BaseColumnType[Char]
//  implicit def doubleColumnType: BaseColumnType[Double] with NumericTypedType
//  implicit def floatColumnType: BaseColumnType[Float] with NumericTypedType
//  implicit def intColumnType: BaseColumnType[Int] with NumericTypedType
//  implicit def longColumnType: BaseColumnType[Long] with NumericTypedType
//  implicit def shortColumnType: BaseColumnType[Short] with NumericTypedType
//  implicit def stringColumnType: BaseColumnType[String]


  implicit val longColumnType = MappedColumnType.base[Long, String](
    //map date to String
    d => d.toString,
    //map String to date
    s => s.toLong
  )


  def configure: Map[String,CrudConfiguration[_,_,_]]


  def getModel(modelName: String): Option[CrudConfiguration[_,_,_]] = configure.get(modelName)


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
