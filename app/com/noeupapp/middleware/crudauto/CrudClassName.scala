package com.noeupapp.middleware.crudauto

//import slick.ast.NumericTypedType
//import slick.driver._
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
//import slick.driver.PostgresDriver.api._

//import slick.driver.PostgresDriver.api.longColumnType
//import slick.lifted.{Isomorphism, TableQuery, Tag}

import scala.language.existentials
import scala.reflect.ClassTag

trait CrudClassName {

  implicit val longColumnType = MappedColumnType.base[Long, String](
    //map date to String
    d => d.toString,
    //map String to date
    s => s.toLong
  )


  protected def configure: Map[String,CrudConfiguration[_,_,_]]


  def getConfiguration: Map[String,CrudConfiguration[_,_,_]] = {
    val duplicateKeys: Set[String] =
      configure.keys.toSet
        .intersect(MiddleCrudAutoConfiguration.configure.keys.toSet)
    assert(duplicateKeys.isEmpty, s"Crud auto config : Some keys ($duplicateKeys) are duplicated! This is probably going to cause trouble hard to debug (Trust me :) )!")
    configure ++ MiddleCrudAutoConfiguration.configure
  }


  def getModel(modelName: String): Option[CrudConfiguration[_,_,_]] =
    getConfiguration.get(modelName)

  val rolesRequiredToGetWithDeleted: List[String] = List("superadmin")

  // TODO : why cast is necessary here ?
  protected def configuration[E, PK, V <: Table[E]](implicit eClass: ClassTag[E], pkClass: ClassTag[PK], vClass: ClassTag[V], baseColumnType: BaseColumnType[PK]) =
      CrudConfiguration[E, PK, V](eClass.runtimeClass.asInstanceOf[Class[E]],
                                  pkClass.runtimeClass.asInstanceOf[Class[PK]],
                                  vClass.runtimeClass.asInstanceOf[Class[V]])
}



case class CrudConfiguration[E, PK, V <: Table[E]](entityClass: Class[E], pK: Class[PK], tableDef: Class[V], authorisation: CrudInterfaceAuthorization = SecuredCrudAuthorization)(implicit val baseColumnType: BaseColumnType[PK]) {

  def withAuth(newAuthorisation: CrudInterfaceAuthorization): CrudConfiguration[E, PK, V] = copy(authorisation = newAuthorisation)

  def withOpenAccess: CrudConfiguration[E, PK, V] = withAuth(OpenCrudAuthorization)

  def withCustomAuth(newFindById: Authorization = SecuredAccess,
               newFindAll: Authorization = SecuredAccess,
               newDeepFindAll: Authorization = SecuredAccess,
               newDeepFindById: Authorization = SecuredAccess,
               newAdd: Authorization = SecuredAccess,
               newUpdate: Authorization = SecuredAccess,
               newDelete: Authorization = SecuredAccess): CrudConfiguration[E, PK, V] =
    copy(authorisation = new CrudInterfaceAuthorization{
                            override val findById: Authorization = newFindById
                            override val findAll: Authorization = newFindAll
                            override val deepFindAll: Authorization = newDeepFindAll
                            override val deepFindById: Authorization = newDeepFindById
                            override val add: Authorization = newAdd
                            override val update: Authorization = newUpdate
                            override val delete: Authorization = newDelete
                          })
}

// TODO : CrudConfiguration fails to compile. CrudConfiguration is needed to validate types
// Ugly trick to make it work
case class CrudConfigurationUnTyped(entityClass: Class[_], pK: Class[_], tableDef: Class[_], baseColumnType: BaseColumnType[_], authorisation: CrudInterfaceAuthorization)



trait CrudInterfaceAuthorization{
  val findById: Authorization = SecuredAccess
  val findAll: Authorization = SecuredAccess
  val deepFindAll: Authorization = SecuredAccess
  val deepFindById: Authorization = SecuredAccess
  val add: Authorization = SecuredAccess
  val update: Authorization = SecuredAccess
  val delete: Authorization = SecuredAccess
}

object SecuredCrudAuthorization extends CrudInterfaceAuthorization

object OpenCrudAuthorization extends CrudInterfaceAuthorization {
  override val findById: Authorization = OpenAccess
  override val findAll: Authorization = OpenAccess
  override val deepFindAll: Authorization = OpenAccess
  override val deepFindById: Authorization = OpenAccess
  override val add: Authorization = OpenAccess
  override val update: Authorization = OpenAccess
  override val delete: Authorization = OpenAccess
}


sealed trait Authorization

trait UserRequired extends Authorization

case object SecuredAccess extends UserRequired
case class SecuredAccessWithRole(roles: String*) extends UserRequired
case object OpenAccess extends Authorization