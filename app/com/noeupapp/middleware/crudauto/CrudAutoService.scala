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
import org.joda.time.format.DateTimeFormat
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._

class CrudAutoService  @Inject()(crudAutoDAO: CrudAutoDAO)() {

  def findById[T](model: Class[T], id: UUID, tableName: String, parser: RowParser[T]): Future[Expect[Option[T]]] = {
    TryBDCall{ implicit c=>
      val newEntity: Option[T] = crudAutoDAO.findById(id, tableName, parser)
      \/-(newEntity)
    }
  }

  def findAll[T](model: Class[T], tableName: String, parser: RowParser[T]): Future[Expect[List[T]]] = {
    TryBDCall{ implicit c=>
      val newEntity:List[T] = crudAutoDAO.findAll(tableName, parser)
      \/-(newEntity)
    }
  }

  def add[T, A](model: Class[T], singleton: Class[A], json: JsObject, tableName: String, parser: RowParser[T], format: Format[T]): Future[Expect[Option[T]]] = {
    TryBDCall{ implicit c=>
      implicit val reads = format
      val entity:T = json.as[T]
      val request = buildAddRequest(entity, singleton, tableName)
      crudAutoDAO.add(tableName, entity, singleton, request._1, request._2)
      \/-(Some(entity))
    }
  }

  def update[T, A](model: Class[T], singleton: Class[A], json: JsObject, id: UUID, tableName: String, parser: RowParser[T], format: Format[T]): Future[Expect[Option[T]]] = {
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
    for {
      js1 <- EitherT(completeId(json))
      js2 <- EitherT(completeDeleted(js1))
      js3 <- EitherT(completeTime(js2))
    }yield js3
  }.run

  def completeUpdate(json: JsObject, id: UUID): Future[Expect[JsObject]] = {
    for {
      js1 <- EitherT(completeDeleted(json+(("id", JsString(id.toString)))))
      js2 <- EitherT(completeTime(js1))
    }yield js2
  }.run

  def completeDeleted(json: JsObject): Future[Expect[JsObject]] = {
    (json \ "deleted").asOpt[Boolean] match {
      case Some(field) => field match {
        case false => Future.successful(\/-(json))
        case _ => Future.successful(\/-(json+(("deleted", JsBoolean(false)))))
      }
      case None => Future.successful(\/-(json+(("deleted", JsBoolean(false)))))
    }
  }

  def completeId(json: JsObject): Future[Expect[JsObject]] = {
    (json \ "id").asOpt[String] match {
      case Some(field) => field match {
        case t if isUUID(t) => Future.successful(\/-(json))
        case _ => Future.successful(\/-(json+(("id", JsString(UUID.randomUUID().toString)))))
      }
      case None => Future.successful(\/-(json+(("id", JsString(UUID.randomUUID().toString)))))
    }
  }

  def completeTime(json: JsObject): Future[Expect[JsObject]] = {
    json.fields.filter(field => field._2.isInstanceOf[JsString] &&
      (field._2.as[String].split(' ').head == "time_plus" |
       field._2.as[String].split(' ').head == "time_minus" |
       field._2.as[String].split(' ').head == "time_now")) match {
      case Nil => Future.successful(\/-(json))
      case fields =>
        val newJs = JsObject(fields.map(f =>
          f._1 -> JsString(getTime(f._2.as[String], f._2.as[String].split(' ').head))))
        Future.successful(\/-(json++newJs))
    }
  }

  def getTime(jsValue: String, operation: String): String = {
    operation match {
      case "time_plus" => jsValue.split(' ')(1) match {
        case "years" => DateTime.now.plusYears(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "months" => DateTime.now.plusMonths(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "weeks" => DateTime.now.plusWeeks(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "days" => DateTime.now.plusDays(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "hours" => DateTime.now.plusHours(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "minutes" => DateTime.now.plusMinutes(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "seconds" => DateTime.now.plusSeconds(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "millis" => DateTime.now.plusMillis(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case _ => DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
      }
      case "time_minus" => jsValue.split(' ')(1) match {
        case "years" => DateTime.now.minusYears(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "months" => DateTime.now.minusMonths(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "weeks" => DateTime.now.minusWeeks(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "days" => DateTime.now.minusDays(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "hours" => DateTime.now.minusHours(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "minutes" => DateTime.now.minusMinutes(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "seconds" => DateTime.now.minusSeconds(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case "millis" => DateTime.now.minusMillis(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
        case _ => DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
      }
      case "time_now" => DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }
  }

  def isUUID(value: String): Boolean = {
    try{
      UUID.fromString(value)
      true
    } catch{
      case e:IllegalArgumentException => false
    }
  }

  def buildAddRequest[T, A](entity: T, singleton: Class[A], tableName : String): (String, String) = {
    val const = singleton.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val getTableColumnNames = singleton.getDeclaredMethod("getTableColumns", classOf[String])
    getTableColumnNames.setAccessible(true)
    val fields = entity.getClass.getDeclaredFields
    val params = fields.flatMap{field => getTableColumnNames.invoke(obj, field.getName).asInstanceOf[Option[String]]}
    val values = fields.map{field => field.setAccessible(true)
                                     (field.get(entity), field.getGenericType.getTypeName)}
    Logger.debug(values.toSeq.toString)
    val value = concatValue(values.toList)
    Logger.debug(value)
    val param = concatParam(params.toList)
    (param, value)
  }

  def buildUpdateRequest[T, A](entity: T, singleton: Class[A], tableName : String): String = {
    val const = singleton.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val getTableColumnNames = singleton.getDeclaredMethod("getTableColumns", classOf[String])
    getTableColumnNames.setAccessible(true)
    val fields = entity.getClass.getDeclaredFields
    val params = fields.map{field =>  field.setAccessible(true)
                                      (getTableColumnNames.invoke(obj, field.getName).asInstanceOf[Option[String]],
                                       field.get(entity),
                                       field.getGenericType.getTypeName)}
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
        case (Some(name),v,t) if !(name == "id") =>
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

  def getClassInfo[T, A](model: Class[T], singleton: Class[A], className: String): Future[Expect[(String, RowParser[T], Format[T])]] = {
    val const = singleton.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val table = singleton.getDeclaredField("tableName")
    table.setAccessible(true)
    val parse = singleton.getDeclaredField("parse")
    parse.setAccessible(true)
    val jsFormat = singleton.getDeclaredField(className.split('.').last+"Format")
    jsFormat.setAccessible(true)
    val format = jsFormat.get(obj).asInstanceOf[Format[T]]
    val parser = parse.get(obj).asInstanceOf[anorm.RowParser[T]]
    val name = table.get(singleton.cast(obj)).asInstanceOf[String]
    Future.successful(\/-((name, parser, format)))
  }

  def toJsValue[T](newObject: List[T], className: String, format: Format[T]): Future[Expect[JsValue]] = {
    implicit val reads = format
    Future.successful(\/-(Json.toJson(newObject)))
  }
}