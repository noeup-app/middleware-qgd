/*
package crud

private val name = typeLit.getType.getTypeName

  private val that = this

  private def abstractCrudService = new AbstractCrudService {
    override protected def getClassName(model: String): Future[Expect[String]] =
      Future.successful(\/-(name))

    override val crudAutoService: CrudAutoService = that.crudAutoService
  }

  def findAll()(implicit uReads: Reads[U]): Future[Expect[List[U]]] =
    abstractCrudService.findAllFlow("").map{
      case \/-(elements) =>
        elements.validate(Reads.list(uReads)) match {
          case JsSuccess(res, _) => \/-(res)
          case JsError(errors)   =>
            Logger.error(s"JsValue.validate error : ${errors.mkString(", ")}")
            -\/(FailError("Unable to validate JsValue"))
        }
      case e@ -\/(_) => e
    }

  def findById(id: UUID)(implicit uReads: Reads[U]): Future[Expect[Option[U]]] =
    abstractCrudService.findByIdFlow("", id).map{
      case \/-(element) =>
        element.validateOpt(uReads) match {
          case JsSuccess(res, _) => \/-(res)
          case JsError(errors)   =>
            Logger.error(s"JsValue.validate error : ${errors.mkString(", ")}")
            -\/(FailError("Unable to validate JsValue"))
        }
      case e@ -\/(_) => e
    }



}

trait AbstractCrudService {

  protected def getClassName(model: String): Future[Expect[String]]

  val crudAutoService: CrudAutoService

  def findByIdFlow(model:String, id: UUID): Future[Expect[JsValue]] = {
    for {
      name <- EitherT(getClassName(model))
      ref = Class.forName(name)
      singleton = Class.forName(name+"$")
      input = Class.forName(name+"In")
      out = Class.forName(name+"Out")

      classInfo <- EitherT(crudAutoService.getClassInfo(ref, singleton, name, input))
      found <- EitherT(crudAutoService.findById(ref, id, classInfo._1, classInfo._2))
      json <- EitherT(crudAutoService.toJsValue(found, ref, singleton, out))
    } yield json
  }.run

  def findAllFlow(model:String): Future[Expect[JsValue]] = {
    for {
      name <- EitherT(getClassName(model))
      ref = Class.forName(name)
      singleton = Class.forName(name+"$")
      input = Class.forName(name+"In")
      out = Class.forName(name+"Out")

      classInfo <- EitherT(crudAutoService.getClassInfo(ref, singleton, name, input))
      found <- EitherT(crudAutoService.findAll(ref, classInfo._1, classInfo._2))
      json <- EitherT(crudAutoService.toJsValue(found, ref, singleton, out))
    } yield json
  }.run

  def addFlow(model: String, json: JsObject): Future[Expect[JsValue]] = {
    for {
      name <- EitherT(getClassName(model))
      ref = Class.forName(name)
      singleton = Class.forName(name+"$")
      input = Class.forName(name+"In")
      out = Class.forName(name+"Out")

      classInfo <- EitherT(crudAutoService.getClassInfo(ref, singleton, name, input))
      validatedJs <- EitherT(crudAutoService.jsonValidate(json, input, classInfo._4))
      jsToAdd <- EitherT(crudAutoService.completeAdd(ref, input, singleton, validatedJs, classInfo._3, classInfo._4))
      found <- EitherT(crudAutoService.add(ref, singleton, jsToAdd, classInfo._1, classInfo._2, classInfo._3))
      newJson <- EitherT(crudAutoService.toJsValue(found, ref, singleton, out))
    } yield newJson
  }.run

  def updateFlow(model: String, json: JsObject, id: UUID): Future[Expect[JsValue]] = {
    for {
      name <- EitherT(getClassName(model))
      ref = Class.forName(name)
      singleton = Class.forName(name+"$")
      input = Class.forName(name+"In")
      out = Class.forName(name+"Out")

      classInfo <- EitherT(crudAutoService.getClassInfo(ref, singleton, name, input))
      validatedJs <- EitherT(crudAutoService.jsonUpdateValidate(json, input, classInfo._4))
      foundOpt <- EitherT(crudAutoService.findById(ref, id, classInfo._1, classInfo._2))
      found <- EitherT(foundOpt |> "couldn't find this entity")
      jsToUpdate <- EitherT(crudAutoService.completeUpdate(found, validatedJs, id, classInfo._3))
      update <- EitherT(crudAutoService.update(ref, singleton, jsToUpdate, id, classInfo._1, classInfo._2, classInfo._3))
      newJson <- EitherT(crudAutoService.toJsValue(update, ref, singleton, out))
    } yield newJson
  }.run

  def deleteFlow(model:String, id: UUID): Future[Expect[Boolean]] = {
    for {
      name <- EitherT(getClassName(model))
      ref = Class.forName(name)
      singleton = Class.forName(name+"$")
      input = Class.forName(name+"In")

      classInfo <- EitherT(crudAutoService.getClassInfo(ref, singleton, name, input))
      foundOpt <- EitherT(crudAutoService.findById(ref, id, classInfo._1, classInfo._2))
      found <- EitherT(foundOpt |> "couldn't find this entity")
      del <- EitherT(crudAutoService.delete(id, classInfo._1))
    } yield del
  }.run

}
*/

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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import slick.driver._
import slick.driver.PostgresDriver.api._
import slick.lifted.TableQuery

trait AbstractCrudService {

  val crudAutoService: CrudAutoService

//  def findByIdFlow(model:String, id: UUID): Future[Expect[JsValue]] = {
//    for {
//      entity <- EitherT(getClassName(model))
//      found <- EitherT(crudAutoService.findById(entity.entityClass, id, entity.tableQuery)
////      json <- EitherT(crudAutoService.toJsValue(found, ref, singleton, out))
//    } yield JsArray
//  }.run

  def findAllFlow(model:String): Future[Expect[JsValue]] =
    {
      for {
        name <- EitherT(getClassName(model))
        entityClass = Class.forName(name.entityClass)
        pkClass = Class.forName(name.pK)
        tableDefClass = Class.forName(name.tableDef)
        singleton = Class.forName(name.entityClass + "$")
//
//        input = Class.forName(name+"In")
        out = Class.forName(name.entityClass + "Out")

        tableQuery = TableQuery(tag =>
          tableDefClass.getConstructor(classOf[Tag])
            .newInstance(tag)
            .asInstanceOf[PKTable[Entity, BaseColumnType[_]]])
        //.asInstanceOf[slick.lifted.AbstractTable[?]])
        //        tableDefClass.getConstructor(classOf[Tag]).newInstance(tag))

        found <- EitherT(crudAutoService.findAll(tableQuery))
        newJson <- EitherT(crudAutoService.toJsValue(found.toList, entityClass, singleton, out))
      } yield newJson
  }.run
//
//  def addFlow(model: String, json: JsObject): Future[Expect[JsValue]] = {
//    for {
//      name <- EitherT(getClassName(model))
//      entityClass = Class.forName(name.entityClass)
//      pkClass = Class.forName(name.pK)
//      tableDefClass = Class.forName(name.tableDef)
//
//      input = Class.forName(name+"In")
//      out = Class.forName(name+"Out")
//
//      tableQuery = TableQuery(tag =>
//        tableDefClass.getConstructor(classOf[Tag])
//          .newInstance(tag)
//          .asInstanceOf[PKTable[Entity, BaseColumnType[_]]])
//          //.asInstanceOf[slick.lifted.AbstractTable[?]])
////        tableDefClass.getConstructor(classOf[Tag]).newInstance(tag))
//
//
////      classInfo <- EitherT(crudAutoService.getClassInfo(ref, singleton, name, input))
////      validatedJs <- EitherT(crudAutoService.jsonValidate(json, input, classInfo._4))
////      jsToAdd <- EitherT(crudAutoService.completeAdd(ref, input, singleton, validatedJs, classInfo._3, classInfo._4))
//      found <- EitherT(crudAutoService.findAll(tableQuery))
////      newJson <- EitherT(crudAutoService.toJsValue(found, ref, singleton, out))
//    } yield Json.obj()
//  }.run
//
//  def updateFlow(model: String, json: JsObject, id: UUID): Future[Expect[JsValue]] = {
//    for {
//      name <- EitherT(getClassName(model))
//      ref = Class.forName(name)
//      singleton = Class.forName(name+"$")
//      input = Class.forName(name+"In")
//      out = Class.forName(name+"Out")
//
//      classInfo <- EitherT(crudAutoService.getClassInfo(ref, singleton, name, input))
//      validatedJs <- EitherT(crudAutoService.jsonUpdateValidate(json, input, classInfo._4))
//      foundOpt <- EitherT(crudAutoService.findById(ref, id, classInfo._1, classInfo._2))
//      found <- EitherT(foundOpt |> "couldn't find this entity")
//      jsToUpdate <- EitherT(crudAutoService.completeUpdate(found, validatedJs, id, classInfo._3))
//      update <- EitherT(crudAutoService.update(ref, singleton, jsToUpdate, id, classInfo._1, classInfo._2, classInfo._3))
//      newJson <- EitherT(crudAutoService.toJsValue(update, ref, singleton, out))
//    } yield newJson
//  }.run
//
//  def deleteFlow(model:String, id: UUID, purge:Option[Boolean]): Future[Expect[Boolean]] = {
//    for {
//      name <- EitherT(getClassName(model))
//      ref = Class.forName(name)
//      singleton = Class.forName(name+"$")
//      input = Class.forName(name+"In")
//
//      classInfo <- EitherT(crudAutoService.getClassInfo(ref, singleton, name, input))
//      foundOpt <- EitherT(crudAutoService.findById(ref, id, classInfo._1, classInfo._2))
//      found <- EitherT(foundOpt |> "couldn't find this entity")
//      del <- EitherT(crudAutoService.delete(id, classInfo._1, purge))
//    } yield del
//  }.run

  protected def getClassName(model: String): Future[Expect[CrudClassNameValue]]

}



//class CrudServiceFactory[T, U] @Inject()(val crudAutoService: CrudAutoService)(implicit val typeLit: TypeLiteral[T]) {
//
//  private val name = typeLit.getType.getTypeName
//
//  private val that = this
//
//  def findAll()(implicit uReads: Reads[U]): Future[Expect[List[U]]] =
//    abstractCrudService.findAllFlow("").map{
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
////  def findById(id: UUID)(implicit uReads: Reads[U]): Future[Expect[Option[U]]] =
////    abstractCrudService.findByIdFlow("", id).map{
////      case \/-(element) =>
////        element.validateOpt(uReads) match {
////          case JsSuccess(res, _) => \/-(res)
////          case JsError(errors)   =>
////            Logger.error(s"JsValue.validate error : ${errors.mkString(", ")}")
////            -\/(FailError("Unable to validate JsValue"))
////        }
////      case e@ -\/(_) => e
////    }
//
//  private def abstractCrudService = new AbstractCrudService {
//    override protected def getClassName[E <: Entity, PK: BaseColumnType, V <: PKTable[E, PK]](model: String): Future[Expect[String]] =
//      Future.successful(\/-(name))
//
//    override val crudAutoService: CrudAutoService = that.crudAutoService
//  }
//
//
//
//}

class CrudService @Inject()(val crudAutoService: CrudAutoService,
                            crudClassName: CrudClassName) extends AbstractCrudService{

  override def getClassName(model: String): Future[Expect[CrudClassNameValue]] = {
    crudClassName.getClassNames(model) match {
      case Some(className) => Future.successful(\/-(className))
      case None => Future.successful(-\/(FailError("this model is not supported")))
    }
  }
}


