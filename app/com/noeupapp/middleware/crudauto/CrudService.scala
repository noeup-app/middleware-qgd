package com.noeupapp.middleware.crudauto

import java.util.UUID
import javax.inject.Inject

import com.google.inject.TypeLiteral
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.TypeCustom._
import play.api.Logger
import play.api.libs.json._
import slick.ast.BaseTypedType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import slick.driver._
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import slick.lifted.TableQuery

import scala.language.existentials

trait AbstractCrudService {

  val crudAutoService: CrudAutoService

  def findByIdFlow(model:String, id: UUID): Future[Expect[Option[JsValue]]] =
    {
      for {
        configuration <- EitherT(getConfiguration(model))
        entityClass = configuration.entityClass
        tableDefClass = configuration.tableDef
        singleton = Class.forName(configuration.entityClass.getName + "$")

        out = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery = TableQuery(tag =>
          tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
            .newInstance(tag)
            .asInstanceOf[Table[Entity] with PKTable[Object]])

        found <- EitherT(
          crudAutoService.find(tableQuery, id)
          (configuration.baseColumnType.asInstanceOf[BaseColumnType[Object]])
        )
        newJson <- EitherT(crudAutoService.toJsValueOpt(found, entityClass.asInstanceOf[Class[Any]], singleton, out))
      } yield newJson
    }.run


  def findAllFlow(model:String): Future[Expect[JsValue]] =
    {
      for {
        configuration <- EitherT(getConfiguration(model))
        entityClass = configuration.entityClass
        tableDefClass = configuration.tableDef
        singleton = Class.forName(configuration.entityClass.getName + "$")

        out = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery = TableQuery(tag =>
          tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
            .newInstance(tag)
            .asInstanceOf[Table[Entity] with PKTable[BaseColumnType[_]]])

        found <- EitherT(crudAutoService.findAll(tableQuery))
        newJson <- EitherT(crudAutoService.toJsValueList(found.toList, entityClass, singleton, out))
      } yield newJson
    }.run

    def addFlow(model: String, json: JsObject): Future[Expect[JsValue]] = {
      for {
        configuration <- EitherT(getConfiguration(model))
        entityClass = configuration.entityClass
        tableDefClass = configuration.tableDef
        singleton = Class.forName(configuration.entityClass.getName + "$")

        input = Class.forName(configuration.entityClass.getName + "In")
        out = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery = TableQuery(tag =>
          tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
            .newInstance(tag)
            .asInstanceOf[Table[Entity] with PKTable[BaseColumnType[_]]])

        classInfo   <- EitherT(crudAutoService.getClassInfo(entityClass, singleton, entityClass.getName, input))
        entityIn <- EitherT(crudAutoService.jsonValidate(json, input, classInfo._4))
        entityToInsert     <- EitherT(crudAutoService.completeAdd(entityClass, input, singleton, entityIn, classInfo._3, classInfo._4))
        found       <- EitherT(
          crudAutoService.add(tableQuery, entityToInsert.asInstanceOf[Entity])
          (configuration.baseColumnType.asInstanceOf[BaseColumnType[Object]]))
        newJson     <- EitherT(crudAutoService.toJsValue(found, entityClass.asInstanceOf[Class[Any]], singleton, out))
      } yield newJson
    }.run

    def updateFlow(model: String, json: JsObject, id: UUID): Future[Expect[JsValue]] = {
      for {
        configuration <- EitherT(getConfiguration(model))
        entityClass = configuration.entityClass
        tableDefClass = configuration.tableDef
        singleton = Class.forName(configuration.entityClass.getName + "$")

        input = Class.forName(configuration.entityClass.getName + "In")
        out = Class.forName(configuration.entityClass.getName + "Out")

        tableQuery = TableQuery(tag =>
          tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
            .newInstance(tag)
            .asInstanceOf[Table[Entity] with PKTable[Object]])

        classInfo   <- EitherT(crudAutoService.getClassInfo(entityClass, singleton, entityClass.getName, input))
        entityIn <- EitherT(crudAutoService.jsonUpdateValidate(json, input, classInfo._4))
        foundOpt <- EitherT(
          crudAutoService.find(tableQuery, id)
          (configuration.baseColumnType.asInstanceOf[BaseColumnType[Object]])
        )
        found <- EitherT(foundOpt |> "Unable to find entity")
        entityToUpdate     <- EitherT(crudAutoService.completeUpdate(found, entityIn, id, classInfo._3.asInstanceOf[Format[Any]]))
        found       <- EitherT(
          crudAutoService.update(tableQuery, id.asInstanceOf[Object], entityToUpdate.asInstanceOf[Entity])
          (configuration.baseColumnType.asInstanceOf[BaseColumnType[Object]]))
        newJson     <- EitherT(crudAutoService.toJsValue(Some(found), entityClass.asInstanceOf[Class[Any]], singleton, out))
      } yield newJson
    }.run

    def deleteFlow[T](model:String, id: T, purge:Option[Boolean]): Future[Expect[Option[T]]] = {
      for {
        configuration <- EitherT(getConfiguration(model))
        tableDefClass = configuration.tableDef

        tableQuery = TableQuery(tag =>
          tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
            .newInstance(tag)
            .asInstanceOf[Table[Entity] with PKTable[Any]])

        foundOpt <- EitherT(
          crudAutoService.find(tableQuery, id)
          (configuration.baseColumnType.asInstanceOf[BaseColumnType[Any]])
        )
        _ <- EitherT(foundOpt |> ("couldn't find this entity", NotFound))

        _ <- EitherT(
          crudAutoService.delete(tableQuery, id)
          (configuration.baseColumnType.asInstanceOf[BaseColumnType[Any]])
        )
      } yield Some(id)
    }.run map {
      case -\/(error) if error.errorType == NotFound => \/-(None)
      case res => res
    }

  protected def getConfiguration(model: String): Future[Expect[CrudConfigurationUnTyped]]

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

class CrudService @Inject()(val crudAutoService: CrudAutoService,
                            crudClassName: CrudClassName) extends AbstractCrudService{

  override def getConfiguration(model: String): Future[Expect[CrudConfigurationUnTyped]] = {
    crudClassName.configure(model) match {
      case Some(className) => Future.successful(\/- {
        CrudConfigurationUnTyped(
          entityClass    = className.entityClass,
          pK             = className.pK,
          tableDef       = className.tableDef,
          baseColumnType = className.baseColumnType
        )
      })
      case None => Future.successful(-\/(FailError("this model is not supported")))
    }
  }
}


