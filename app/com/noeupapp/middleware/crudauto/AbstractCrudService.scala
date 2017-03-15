package com.noeupapp.middleware.crudauto

import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.entities.role.RoleService
import com.noeupapp.middleware.entities.user.User
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.TypeCustom._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import slick.lifted.TableQuery
import play.api.mvc.Results._


class AbstractCrudService @Inject() (crudAutoService: CrudAutoService,
                                     crudClassName: CrudClassName,
                                     roleService: RoleService){


  def parseStringToType[T](strClass: Class[T], str: String): Future[Expect[T]] = Future {
    try {
      strClass match {
        case s if s == classOf[UUID] => \/-(UUID.fromString(str).asInstanceOf[T])
        case s if s == classOf[Int]  => \/-(str.toInt.asInstanceOf[T])
        case s if s == classOf[Long] => \/-(str.toLong.asInstanceOf[T])
        case s if s == classOf[String] => \/-(str.asInstanceOf[T])
        case _ => -\/(FailError(s"id type known (id given : `$str` ; expected `$strClass`)", errorType = BadRequest))
      }
    }catch {
      case _: Exception => -\/(FailError(s"id type is not what I was expected (id given : `$str` ; expected `$strClass`)", errorType = BadRequest))
    }
  }

  def checkIfAuthorized(method: Authorization, userOpt: Option[User]): Future[Expect[Unit]] = (method, userOpt) match {
    case (_: UserRequired, None) =>
      Future.successful(-\/(FailError("Not authorized", errorType = Unauthorized)))
    case (sawr: SecuredAccessWithRole, Some(user)) =>
      {
        for (userRoles <- EitherT(roleService.getRolesByUser(user.id)))
          yield userRoles.intersect(sawr.roles).isEmpty
      }.run.map{
        case \/-(true) => -\/(FailError("Not authorized", errorType = Unauthorized))
        case \/-(false) => \/-(())
        case error @ -\/(_) => error
      }
    case _                     => Future.successful(\/-(()))
  } 
  
  
  
  def checkIfAuthorizedWithDelete(model: String, accountOpt: Option[Account], withDeleteOpt: Option[Boolean]): Future[Expect[Boolean]] = {

    def userHasSufficientRoles(usersRoles: List[String]): Boolean =
      (crudClassName.rolesRequiredToGetWithDeleted diff usersRoles).nonEmpty
    
    accountOpt -> withDeleteOpt match {
      case (Some(account), Some(true)) if userHasSufficientRoles(account.roles) => Future.successful(\/-(true))
      case (_, Some(true)) => Future.successful(-\/(FailError(s"You don't have sufficient right to get $model with deleted column")))
      case _ => Future.successful(\/-(false))
    }
  }
  
  
  
  
  
  
  def findByIdFlow(model:String, 
                   rawId: String, 
                   omits: List[String], 
                   includes: List[String], 
                   userOpt: Option[User]): Future[Expect[Option[JsValue]]] =
    {
      for {
        configuration <- EitherT(getConfiguration(model))

        _             <- EitherT(checkIfAuthorized(configuration.authorisation.findById, userOpt))

        entityClass   = configuration.entityClass
        tableDefClass = configuration.tableDef
        id            <- EitherT(parseStringToType(configuration.pK, rawId))

        singleton     = Class.forName(configuration.entityClass.getName + "$")
        out           = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery    = TableQuery(tag =>
                          tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
                            .newInstance(tag)
                            .asInstanceOf[Table[Entity[Any]] with PKTable])

        found         <- EitherT(
                          crudAutoService.find(tableQuery, id.asInstanceOf[Any])
                          (configuration.baseColumnType.asInstanceOf[BaseColumnType[Any]]))
        newJson       <- EitherT(crudAutoService.toJsValueOpt(found, entityClass.asInstanceOf[Class[Any]], singleton, out))
        filteredJson  <- EitherT(crudAutoService.filterOmitsAndRequiredFieldsOfJsValue(newJson, omits, includes))
      } yield filteredJson
    }.run

  //def findAllFlow(model:String, omits: List[String], includes: List[String], search: Option[String], countOnly: Boolean, p: Option[Int], pp: Option[Int]): Future[Expect[JsValue]] =

  def findAllFlow(model:String,
                  omits: List[String],
                  includes: List[String],
                  search: Option[String],
                  countOnly: Boolean,
                  p: Option[Int],
                  pp: Option[Int],
                  accountOpt: Option[Account],
                  withDeleteOpt: Option[Boolean]): Future[Expect[JsValue]] =
    {
      val userOpt = accountOpt.map(_.user)

      for {
        configuration <- EitherT(getConfiguration(model))

        _             <- EitherT(checkIfAuthorized(configuration.authorisation.findAll, userOpt))
        withDelete    <- EitherT(checkIfAuthorizedWithDelete(model, accountOpt, withDeleteOpt))

        entityClass   = configuration.entityClass
        tableDefClass = configuration.tableDef

        singleton     = Class.forName(configuration.entityClass.getName + "$")

        bareEntity    = Class.forName(configuration.entityClass.getName)
        input         = Class.forName(configuration.entityClass.getName + "In")
        out           = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery    = provideTableQuery(tableDefClass)

        classInfo     <- EitherT(crudAutoService.getClassInfo(entityClass, singleton, entityClass.getName, input))

        found         <- EitherT(crudAutoService.findAll(tableQuery, search, countOnly, p, pp, withDelete)
                                                        (classInfo.jsonFormat.asInstanceOf[Format[Entity[Any]]]))
        count         = found.length

        outClass      = if (withDelete) bareEntity else out

        newJson       <- EitherT(crudAutoService.toJsValueList(found.toList, entityClass, singleton, outClass, withDelete))
        filteredJson  <- EitherT(crudAutoService.filterOmitsAndRequiredFieldsOfJsValue(newJson, omits, includes))
      } yield if (countOnly) Json.toJson(count) else filteredJson
    }.run


  def provideTableQuery(tableDefClass: Class[_]): TableQuery[Table[Entity[Any]] with PKTable] =
    TableQuery(tag =>
      tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
        .newInstance(tag)
        .asInstanceOf[Table[Entity[Any]] with PKTable])

  def deepFetchAllFlow(model1: String, 
                       rawId: String, 
                       model2: String, 
                       omits: List[String], 
                       includes: List[String], 
                       search: Option[String], 
                       countOnly: Boolean, 
                       p: Option[Int], 
                       pp: Option[Int],
                       accountOpt: Option[Account],
                       withDeleteOpt: Option[Boolean]): Future[Expect[Option[JsValue]]] =
    {
      val userOpt = accountOpt.map(_.user)
      
      for {

        entity1Found  <- EitherT(this.findByIdFlow(model1, rawId, Nil, Nil, userOpt))
        _             <- EitherT(entity1Found |> (s"`/$model1/$rawId` is not found", NotFound))

        configuration <- EitherT(getConfiguration(model2))

        _             <- EitherT(checkIfAuthorized(configuration.authorisation.deepFindAll, userOpt))
        withDelete    <- EitherT(checkIfAuthorizedWithDelete(s"$model1/$rawId/$model2", accountOpt, withDeleteOpt))


                                                                                                                                id            <- EitherT(parseStringToType(configuration.pK, rawId))

        entityClass   = configuration.entityClass
        tableDefClass = configuration.tableDef
        singleton     = Class.forName(configuration.entityClass.getName + "$")

        bareEntity    = Class.forName(configuration.entityClass.getName)
        input         = Class.forName(configuration.entityClass.getName + "In")
        out           = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery2   = TableQuery(tag =>
                          tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
                            .newInstance(tag)
                            .asInstanceOf[Table[Entity[Any]] with PKTable])

        fkOpt         = tableQuery2.baseTableRow.foreignKeys.find(_.name == s"${model1}_fkey")
        fk            <- EitherT(fkOpt |> s"Foreign key (`${model1}_fkey`) is not found")
        classInfo     <- EitherT(crudAutoService.getClassInfo(entityClass, singleton, entityClass.getName, input))

        found         <- EitherT(
                          crudAutoService.deepFindAll(tableQuery2, id, fk, search, p, pp)
                          (classInfo.jsonFormat.asInstanceOf[Format[Entity[Any]]]))

        count         = found.length

        outClass      = if (withDelete) bareEntity else out

        newJson       <- EitherT(crudAutoService.toJsValueList(found.toList, entityClass, singleton, outClass, false))
        filteredJson  <- EitherT(crudAutoService.filterOmitsAndRequiredFieldsOfJsValue(newJson, omits, includes))
      } yield if (countOnly) Json.toJson(count) else filteredJson
    }.run map {
      case -\/(error) if error.errorType == NotFound => \/-(None)
      case error @ -\/(_) => error
      case \/-(res) => \/-(Some(res))
    }

  def deepFetchByIdFlow(model1: String, 
                        rawId1: String, 
                        model2: String, 
                        rawId2: String, 
                        omits: List[String], 
                        includes: List[String], 
                        userOpt: Option[User]): Future[Expect[Option[JsValue]]] =
    {
      for {
        entity1Found    <- EitherT(this.findByIdFlow(model1, rawId1, Nil, Nil, userOpt))
        _               <- EitherT(entity1Found |> (s"`/$model1/$rawId1` is not found", NotFound))

        configuration   <- EitherT(getConfiguration(model2))

        _               <- EitherT(checkIfAuthorized(configuration.authorisation.deepFindById, userOpt))

        id1             <- EitherT(parseStringToType(configuration.pK, rawId1))
        id2             <- EitherT(parseStringToType(configuration.pK, rawId2))

        entityClass     = configuration.entityClass
        tableDefClass   = configuration.tableDef
        singleton       = Class.forName(configuration.entityClass.getName + "$")

        input           = Class.forName(configuration.entityClass.getName + "In")
        out             = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery2     = TableQuery(tag =>
                            tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
                              .newInstance(tag)
                              .asInstanceOf[Table[Entity[Any]] with PKTable])

        fkOpt           = tableQuery2.baseTableRow.foreignKeys.find(_.name == s"${model1}_fkey")

        fk              <- EitherT(fkOpt |> s"Foreign key (`${model1}_fkey`) is not found")

        classInfo       <- EitherT(crudAutoService.getClassInfo(entityClass, singleton, entityClass.getName, input))

        found           <- EitherT(
                            crudAutoService.deepFindById(tableQuery2, id1, fk, id2)
                            (classInfo.jsonFormat.asInstanceOf[Format[Entity[Any]]]))
        newJson      <- EitherT(crudAutoService.toJsValueOpt(found, entityClass.asInstanceOf[Class[Any]], singleton, out))
        filteredJson <- EitherT(crudAutoService.filterOmitsAndRequiredFieldsOfJsValue(newJson, omits, includes))
      } yield filteredJson
    }.run map {
      case -\/(error) if error.errorType == NotFound => \/-(None)
      case error @ -\/(_) => error
      case \/-(res) => \/-(res)
    }

    def addFlow(model: String, json: JsObject, userOpt: Option[User]): Future[Expect[JsValue]] = {
      for {
        configuration   <- EitherT(getConfiguration(model))

        _               <- EitherT(checkIfAuthorized(configuration.authorisation.add, userOpt))

        entityClass     = configuration.entityClass
        tableDefClass   = configuration.tableDef
        singleton       = Class.forName(configuration.entityClass.getName + "$")

        input           = Class.forName(configuration.entityClass.getName + "In")
        out             = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery      = TableQuery(tag =>
                            tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
                              .newInstance(tag)
                              .asInstanceOf[Table[Entity[Any]] with PKTable])

        classInfo       <- EitherT(crudAutoService.getClassInfo(entityClass, singleton, entityClass.getName, input))
        entityIn        <- EitherT(crudAutoService.jsonValidate(json, input, classInfo.jsonInFormat))
        entityToInsert  <- EitherT(crudAutoService.completeAdd(entityClass, input, singleton, entityIn, classInfo.jsonFormat, classInfo.jsonInFormat))

        found           <- EitherT(
                            crudAutoService.add(tableQuery, entityToInsert.asInstanceOf[Entity[Any]])
                            (configuration.baseColumnType.asInstanceOf[BaseColumnType[Object]]))
        newJson         <- EitherT(crudAutoService.toJsValue(found, entityClass.asInstanceOf[Class[Any]], singleton, out))
      } yield newJson
    }.run

    def updateFlow(model: String, json: JsObject, rawId: String, userOpt: Option[User]): Future[Expect[Option[JsValue]]] = {
      for {

        //entity1Found    <- EitherT(this.findByIdFlow(model, id, Nil, Nil))
        //_               <- EitherT(entity1Found |> (s"`/$model/$id` is not found", NotFound))
        configuration   <- EitherT(getConfiguration(model))

        _               <- EitherT(checkIfAuthorized(configuration.authorisation.update, userOpt))

        id              <- EitherT(parseStringToType(configuration.pK, rawId))

        entityClass     = configuration.entityClass
        tableDefClass   = configuration.tableDef

        singleton       = Class.forName(configuration.entityClass.getName + "$")
        input           = Class.forName(configuration.entityClass.getName + "In")
        out             = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery      = TableQuery(tag =>
                            tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
                              .newInstance(tag)
                              .asInstanceOf[Table[Entity[Any]] with PKTable])

        foundOpt        <- EitherT(
                            crudAutoService.find(tableQuery, id.asInstanceOf[Any])
                            (configuration.baseColumnType.asInstanceOf[BaseColumnType[Any]]))
        found           <- EitherT(foundOpt |> (s"`/$model/$id` is not found", NotFound))

        classInfo       <- EitherT(crudAutoService.getClassInfo(entityClass, singleton, entityClass.getName, input))
        entityIn        <- EitherT(crudAutoService.jsonUpdateValidate(json, input, classInfo.jsonInFormat))

        entityToUpdate  <- EitherT(crudAutoService.completeUpdate(found, entityIn, classInfo.jsonFormat.asInstanceOf[Format[Any]]))
        updated         <- EitherT(
                            crudAutoService.update(tableQuery, id.asInstanceOf[Object], entityToUpdate.asInstanceOf[Entity[Any]])
                            (configuration.baseColumnType.asInstanceOf[BaseColumnType[Object]]))
        newJson         <- EitherT(crudAutoService.toJsValue(updated, entityClass.asInstanceOf[Class[Any]], singleton, out))
      } yield newJson
    }.run map {
      case -\/(error) if error.errorType == NotFound => \/-(None)
      case error @ -\/(_) => error
      case \/-(res) => \/-(Some(res))
    }

    def deleteFlow(model:String, rawId: String, purge:Option[Boolean], force_delete: Boolean, userOpt: Option[User]): Future[Expect[Option[String]]] = {
      for {
        configuration   <- EitherT(getConfiguration(model))

        _               <- EitherT(checkIfAuthorized(configuration.authorisation.delete, userOpt))

        tableDefClass   = configuration.tableDef
        id              <- EitherT(parseStringToType(configuration.pK, rawId.toString))

        tableQuery      = TableQuery(tag =>
                            tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
                              .newInstance(tag)
                              .asInstanceOf[Table[Entity[Any]] with PKTable])

        foundOpt        <- EitherT(
                              crudAutoService.find(tableQuery, id.asInstanceOf[Any])
                              (configuration.baseColumnType.asInstanceOf[BaseColumnType[Any]]))
        _               <- EitherT(foundOpt |> ("couldn't find this entity", NotFound))
        _               <- EitherT(
                            crudAutoService.delete(tableQuery, id, force_delete)
                            (configuration.baseColumnType.asInstanceOf[BaseColumnType[Any]]))
      } yield Some(rawId)
    }.run map {
      case -\/(error) if error.errorType == NotFound => \/-(None)
      case error @ -\/(_) => error
      case res => res
    }

  protected def getConfiguration(model: String): Future[Expect[CrudConfigurationUnTyped]] = {
    crudClassName.getModel(model) match {
      case Some(className) => Future.successful(\/- {
        CrudConfigurationUnTyped(
          entityClass    = className.entityClass,
          pK             = className.pK,
          tableDef       = className.tableDef,
          baseColumnType = className.baseColumnType,
          authorisation  = className.authorisation
        )
      })
      case None => Future.successful(-\/(FailError("this model is not supported")))
    }
  }

}


//
//class CrudServiceFactory[T, U] @Inject()(val crudAutoService: CrudAutoService,
//                                         crudService: CrudService)(implicit val typeLit: TypeLiteral[T]) {
//
//  private val name = typeLit.getType.getTypeName
//
//  private val that = this
//
//  def findAll()(implicit uReads: Reads[U]): Future[Expect[List[U]]] =
//    crudService.findAllFlow(name).map{
//      case \/-(elements) =>
//        elements.validate(Reads.list(uReads)) match {
//          case JsSuccess(res, _) => \/-(res)
//          case JsError(errors)   =>
//            Logger.error(s"JsValue.validate error : ${errors.mkString(", ")}")
//            -\/(FailError("Unable to validate JsValue"))
//        }
//      case e@ -\/(_) => e
//    }
//
//  def findById(id: UUID)(implicit uReads: Reads[U]): Future[Expect[Option[U]]] =
//    crudService.findByIdFlow(name, id).map{
//      case \/-(element) =>
//        element.validateOpt(uReads) match {
//          case JsSuccess(res, _) => \/-(res)
//          case JsError(errors)   =>
//            Logger.error(s"JsValue.validate error : ${errors.mkString(", ")}")
//            -\/(FailError("Unable to validate JsValue"))
//        }
//      case e@ -\/(_) => e
//    }
//
//
//}
//
//class CrudService @Inject()(val crudAutoService: CrudAutoService,
//                            crudClassName: CrudClassName) extends AbstractCrudService{
//
//  override def getConfiguration(model: String): Future[Expect[CrudConfigurationUnTyped]] = {
//    crudClassName.configure(model) match {
//      case Some(className) => Future.successful(\/- {
//        CrudConfigurationUnTyped(
//          entityClass    = className.entityClass,
//          pK             = className.pK,
//          tableDef       = className.tableDef,
//          baseColumnType = className.baseColumnType
//        )
//      })
//      case None => Future.successful(-\/(FailError("this model is not supported")))
//    }
//  }
//}


