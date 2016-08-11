package com.noeupapp.middleware.crudauto

import java.util.UUID
import javassist.bytecode.stackmap.TypeData.ClassName
import javax.inject.Inject

import anorm.RowParser
import com.noeupapp.middleware.entities.entity.EntityDAO
import com.noeupapp.middleware.entities.entity.Entity
import com.noeupapp.middleware.crudauto.CrudAuto._
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.utils.MonadTransformers._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.StringUtils._
import org.joda.time.DateTime
import play.api.libs.json._
import com.noeupapp.middleware.utils.FutureFunctor._
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._

class CrudAutoService  @Inject()(crudAutoDAO: CrudAutoDAO,
                                 entityDAO: EntityDAO)() {

  def findByIdFlow(model:String, id: UUID): Future[Expect[JsValue]] = {
    for {
      name <- EitherT(getClassName(model))

      singleton = Class.forName("com.noeupapp.middleware.entities.entity."+name+"$")

      ref = Class.forName("com.noeupapp.middleware.entities.entity."+name)
      classInfos <- EitherT(getClassInfos(ref, singleton, name))
      found <- EitherT(findById(ref, id, classInfos._1, classInfos._2))

      json <- EitherT(toJsValue(found.toList, name, classInfos._3))
    } yield json
  }.run

  def findAllFlow(model:String): Future[Expect[JsValue]] = {
    for {
      name <- EitherT(getClassName(model))
      singleton = Class.forName("com.noeupapp.middleware.entities.entity."+name+"$")
      ref = Class.forName("com.noeupapp.middleware.entities.entity."+name)
      classInfos <- EitherT(getClassInfos(ref, singleton, name))

      found <- EitherT(findAll(ref, classInfos._1, classInfos._2))

      json <- EitherT(toJsValue(found, name, classInfos._3))
    } yield json
  }.run

  def addFlow(model: String, json: JsObject): Future[Expect[JsValue]] = {
    for {
      name <- EitherT(getClassName(model))

      ref = Class.forName("com.noeupapp.middleware.entities.entity."+name)
      singleton = Class.forName("com.noeupapp.middleware.entities.entity."+name+"$")
      classInfos <- EitherT(getClassInfos(ref, singleton, name))
      found <- EitherT(add(ref, json, classInfos._1, classInfos._2, classInfos._3))

      json <- EitherT(toJsValue(found.toList, name, classInfos._3))
    } yield json
  }.run

  def getClassName(model: String): Future[Expect[String]] = {
    Logger.debug(model)
    CrudAuto.supportedClasses.find(name => name.equalsIgnoreCase(CrudAuto.toSingular(model).getOrElse(""))) match {
      case Some(className) => Future.successful(\/-(className))
      case None => Future.successful(-\/(FailError("this model is not supported")))
    }
  }

  def findById[T](model: T, id: UUID, tableName: String, parser: RowParser[T]): Future[Expect[Option[T]]] = {
    TryBDCall{ implicit c=>
      val newEntity: Option[T] = crudAutoDAO.findById(id, tableName, parser)
      Logger.debug(newEntity.getClass.getName)
      \/-(newEntity)
    }
  }

  def findAll[T](model: T, tableName: String, parser: RowParser[T]): Future[Expect[List[T]]] = {
    TryBDCall{ implicit c=>
      Logger.debug(model.asInstanceOf[Class[T]].getName)
      val newEntity:List[T] = crudAutoDAO.findAll(tableName, parser)
      Logger.debug(newEntity.getClass.getName)
      \/-(newEntity)
    }
  }

  def add[T](model: T, json: JsObject, tableName: String, parser: RowParser[T], format: Format[T]): Future[Expect[Option[T]]] = {
    TryBDCall{ implicit c=>
      val classe = model.asInstanceOf[Class[T]]
      Logger.debug(model.asInstanceOf[Class[T]].getName)
      implicit val reads = format
      val entity:T = (json+(("id", JsString(UUID.randomUUID().toString)))).as[T]
      Logger.debug(entity.toString)
      crudAutoDAO.add(entity, tableName) match {
        case true => \/-(Some(entity))
        case false => -\/(FailError("couldn't add entity to database"))
      }
    }
  }

  def getClassInfos[T, A](model: T, singleton: A, className: String): Future[Expect[(String, RowParser[T], Format[T])]] = {
    val sing = singleton.asInstanceOf[Class[A]]
    val const = sing.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    Logger.debug(sing.getDeclaredFields.toSeq.toString)
    val table = sing.getDeclaredField("tableName")
    table.setAccessible(true)
    val parse = sing.getDeclaredField("parse")
    parse.setAccessible(true)
    val jsFormat = sing.getDeclaredField(className+"Format")
    jsFormat.setAccessible(true)
    val format = jsFormat.get(obj).asInstanceOf[Format[T]]
    val parser = parse.get(obj).asInstanceOf[anorm.RowParser[T]]
    val name = table.get(sing.cast(obj)).asInstanceOf[String]
    Future.successful(\/-((name, parser, format)))
  }

  def toJsValue[T](newObject: List[T], className: String, format: Format[T]): Future[Expect[JsValue]] = {
    Logger.debug(newObject.getClass.getName)
    implicit val reads = format
    Future.successful(\/-(Json.toJson(newObject)))
  }
}