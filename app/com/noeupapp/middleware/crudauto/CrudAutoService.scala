package com.noeupapp.middleware.crudauto

import java.util.UUID
import javax.inject.Inject

import anorm.RowParser
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.utils.MonadTransformers._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.TypeCustom._
import org.joda.time.DateTime
import play.api.libs.json._
import com.noeupapp.middleware.utils.FutureFunctor._
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._

class CrudAutoService  @Inject()(crudAutoDAO: CrudAutoDAO)() {

  def findById[T](model: T, id: UUID, tableName: String, parser: RowParser[T]): Future[Expect[Option[T]]] = {
    TryBDCall{ implicit c=>
      val newEntity: Option[T] = crudAutoDAO.findById(id, tableName, parser)
      \/-(newEntity)
    }
  }

  def findAll[T](model: T, tableName: String, parser: RowParser[T]): Future[Expect[List[T]]] = {
    TryBDCall{ implicit c=>
      val newEntity:List[T] = crudAutoDAO.findAll(tableName, parser)
      \/-(newEntity)
    }
  }

  def add[T, A](model: T, singleton: A, json: JsObject, tableName: String, parser: RowParser[T], format: Format[T]): Future[Expect[Option[T]]] = {
    TryBDCall{ implicit c=>
      implicit val reads = format
      val entity:T = json.as[T]
      val request = buildAddRequest(entity, singleton, tableName)
      crudAutoDAO.add(tableName, entity, singleton, request._1, request._2)
      \/-(Some(entity))
    }
  }

  def update[T, A](model: T, singleton: A, json: JsObject, id: UUID, tableName: String, parser: RowParser[T], format: Format[T]): Future[Expect[Option[T]]] = {
    TryBDCall{ implicit c=>
      implicit val reads = format
      val entity:T = json.as[T]
      val request = buildUpdateRequest(entity, singleton, tableName)
      crudAutoDAO.update(tableName, request, id)
      \/-(Some(entity))
    }
  }

  def delete(id: UUID, tableName: String): Future[Expect[Boolean]] = {
    TryBDCall{ implicit c=>
      \/-(crudAutoDAO.delete(tableName, id))
    }
  }

  def completeAdd(json: JsObject): Future[Expect[JsObject]] = {
    Future.successful(\/-(json+(("id", JsString(UUID.randomUUID().toString)))+(("deleted", JsBoolean(false)))))
  }

  def completeUpdate(json: JsObject, id: UUID): Future[Expect[JsObject]] = {
    Future.successful(\/-(json+(("id", JsString(id.toString)))+(("deleted", JsBoolean(false)))))
  }

  def buildAddRequest[T, A](entity: T, singleton: A, tableName : String): (String, String) = {
    val sing = singleton.asInstanceOf[Class[A]]
    val const = sing.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val getTableColumnNames = sing.getDeclaredMethod("getTableColumns", classOf[String])
    getTableColumnNames.setAccessible(true)
    val fields = entity.getClass.getDeclaredFields
    val params = fields.flatMap{field => getTableColumnNames.invoke(obj, field.getName).asInstanceOf[Option[String]] }
    val values = fields.map{field => field.setAccessible(true)
                                     (field.get(entity), field.getGenericType.getTypeName)}
    Logger.debug(values.toSeq.toString())
    val value = concatValue(values.toList)
    Logger.debug(value)
    val param = concatParam(params.toList)
    (param, value)
  }

  def buildUpdateRequest[T, A](entity: T, singleton: A, tableName : String): String = {
    val sing = singleton.asInstanceOf[Class[A]]
    val const = sing.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val getTableColumnNames = sing.getDeclaredMethod("getTableColumns", classOf[String])
    getTableColumnNames.setAccessible(true)
    val fields = entity.getClass.getDeclaredFields
    val params = fields.map{field =>  field.setAccessible(true)
                                      (getTableColumnNames.invoke(obj, field.getName).asInstanceOf[Option[String]], field.get(entity), field.getGenericType.getTypeName)}
    Logger.debug(concatParamAndValue(params.toList))
    concatParamAndValue(params.toList)
  }

  def concatParam(params: List[String], param: String = ""): String = {
    params match {
      case x::xs => concatParam(xs, param + x + ", ")
      case Nil => param.splitAt(param.length -2)._1
    }
  }

  def concatValue(values: List[(AnyRef, String)], value: String = ""): String = {
    values match {
      case x::xs => concatValue(xs, value + valueToAdd(x._1.toString, x._2))
      case Nil => value.splitAt(value.length-2)._1
    }
  }

  def concatParamAndValue(list: List[(Option[String], AnyRef, String)], value: String = ""): String = {
    list match {
      case x::xs => x match {
        case (Some(name),v,t) if !name.equals("id") =>
          concatParamAndValue(xs, value + name + " = " + valueToAdd(v.toString, t))
        case _  => concatParamAndValue(xs, value)
      }
      case Nil => value.splitAt(value.length-2)._1
    }
  }

  def valueToAdd(value: String, typeName: String): String = {
    (value, typeName) match {
      case (y, "java.util.UUID") =>
        "'" + y + "'::UUID, "
      case (y,z) if z.contains("scala.Option") && z.contains("UUID") && y.contains("Some") =>
        "'" + y.splitAt(5)._2.splitAt(y.length-6)._1 + "'::UUID, "
      case (y,z) if z.contains("scala.Option") && !z.contains("UUID") && y.contains("Some") =>
        "'" + y.splitAt(5)._2.splitAt(y.length-6)._1 + "', "
      case (y,z) if z.contains("scala.Option") && !y.contains("Some") =>
        "null, "
      case (y,z) => "'" + y + "', "
    }
  }

  def getClassInfo[T, A](model: T, singleton: A, className: String): Future[Expect[(String, RowParser[T], Format[T])]] = {
    val sing = singleton.asInstanceOf[Class[A]]
    val const = sing.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val table = sing.getDeclaredField("tableName")
    table.setAccessible(true)
    val parse = sing.getDeclaredField("parse")
    parse.setAccessible(true)
    val jsFormat = sing.getDeclaredField(className.split('.').last+"Format")
    jsFormat.setAccessible(true)
    val format = jsFormat.get(obj).asInstanceOf[Format[T]]
    val parser = parse.get(obj).asInstanceOf[anorm.RowParser[T]]
    val name = table.get(sing.cast(obj)).asInstanceOf[String]
    Future.successful(\/-((name, parser, format)))
  }

  def toJsValue[T](newObject: List[T], className: String, format: Format[T]): Future[Expect[JsValue]] = {
    implicit val reads = format
    Future.successful(\/-(Json.toJson(newObject)))
  }
}