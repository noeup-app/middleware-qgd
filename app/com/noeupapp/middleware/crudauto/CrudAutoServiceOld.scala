//package com.noeupapp.middleware.crudauto
//
//import java.lang.reflect.Method
//import java.util.UUID
//import javax.inject.Inject
//
//import anorm.RowParser
//import com.noeupapp.middleware.errorHandle.ExceptionEither._
//import com.noeupapp.middleware.errorHandle.FailError
//import com.noeupapp.middleware.errorHandle.FailError.Expect
//import play.api.Logger
//import play.api.libs.json._
//
//import scala.concurrent.Future
//import scala.language.higherKinds
//import scalaz._
//
//class CrudAutoServiceOld  @Inject()(crudAutoDAO: CrudAutoDAO)() {
//
//  def findById[T](model: Class[T], id: UUID, tableName: String, parser: RowParser[T]): Future[Expect[Option[T]]] = {
//    TryBDCall{ implicit c=>
//      val newEntity: Option[T] = crudAutoDAO.findById(id, tableName, parser)
//      \/-(newEntity)
//    }
//  }
//
//  def findAll[T](model: Class[T], tableName: String, parser: RowParser[T]): Future[Expect[List[T]]] = {
//    TryBDCall{ implicit c=>
//      val newEntity:List[T] = crudAutoDAO.findAll(tableName, parser)
//      \/-(newEntity)
//    }
//  }
//
//  def add[T, A](model: Class[T], singleton: Class[A], json: JsObject, tableName: String, parser: RowParser[T], format: Format[T]): Future[Expect[Option[T]]] = {
//    TryBDCall{ implicit c=>
//      implicit val jsFormat = format
//      val entity:T = json.as[T]
//      val request = buildAddRequest(entity, singleton, tableName)
//      crudAutoDAO.add(tableName, entity, singleton, request._1, request._2)
//      \/-(Some(entity))
//    }
//  }
//
//  def update[T, A](model: Class[T], singleton: Class[A], json: JsObject, id: UUID, tableName: String, parser: RowParser[T], format: Format[T]): Future[Expect[Option[T]]] = {
//    TryBDCall{ implicit c=>
//      implicit val jsFormat = format
//      val entity:T = json.as[T]
//      val request = buildUpdateRequest(model, json, singleton, tableName, format)
//      crudAutoDAO.update(tableName, request, id)
//      \/-(Some(entity))
//    }
//  }
//
//  def delete(id: UUID, tableName: String, purge: Option[Boolean]): Future[Expect[Boolean]] = {
//    TryBDCall{ implicit c =>
//      purge match {
//        case Some(true) => crudAutoDAO.purge(tableName, id) match {
//          case 0 => \/-(false)
//          case 1 => \/-(true)
//          case i:Int => -\/(FailError("More than one row deleted (" + i + ")"))
//        }
//        case _ =>
//          crudAutoDAO.delete(tableName, id) match {
//            case 0 => \/-(false)
//            case 1 => \/-(true)
//            case i:Int => -\/(FailError("More than one row deleted (" + i + ")"))
//          }
//      }
//
//
//    }
//  }
//
//  def completeAdd[T,A, B](model: Class[T], in: Class[B], singleton: Class[A], json: JsObject, format: Format[T], formatIn: Format[B]): Future[Expect[JsObject]] = {
//    //Deprecated
//    /*for {
//      js1 <- EitherT(completeId(json))
//      js2 <- EitherT(completeDeleted(js1))
//      js3 <- EitherT(completeTime(js2))
//    }yield js3*/
//    implicit val form = format
//    implicit val formIn  = formatIn
//    val input:B = json.as[B]
//    val consts = model.getDeclaredConstructors
//    val const = consts.find(r=> r.getParameterTypes.contains(in))
//    Logger.debug(const.mkString)
//    const match {
//      case Some(init) => init.setAccessible(true)
//        val entity: T = init.newInstance(input.asInstanceOf[Object]).asInstanceOf[T]
//        Future.successful(\/-(Json.toJson(entity).as[JsObject]))
//      case None => Future.successful(-\/(FailError("couldn't find constructor")))
//    }
//  }//.run
//
//  def completeUpdate[T](entity: T, json: JsObject, id: UUID, format: Format[T]): Future[Expect[JsObject]] = {
//    //Deprecated
//    // for {
//    //   js1 <- EitherT(completeDeleted(json+(("id", JsString(id.toString)))))
//    //   js2 <- EitherT(completeTime(js1))
//    // } yield js2
//    implicit val reads = format
//    val oldJson = Json.toJson(entity).as[JsObject]
//    Future.successful(\/-(oldJson ++ json))
//  }//.run
//
//  //Deprecated
//  /*def completeDeleted(json: JsObject): Future[Expect[JsObject]] = {
//    (json \ "deleted").asOpt[Boolean] match {
//      case Some(field) => field match {
//        case false => Future.successful(\/-(json))
//        case _ => Future.successful(\/-(json+(("deleted", JsBoolean(false)))))
//      }
//      case None => Future.successful(\/-(json+(("deleted", JsBoolean(false)))))
//    }
//  }*/
//
//  //Deprecated
//  /*def completeId(json: JsObject): Future[Expect[JsObject]] = {
//    (json \ "id").asOpt[String] match {
//      case Some(field) => field match {
//        case t if isUUID(t) => Future.successful(\/-(json))
//        case _ => Future.successful(\/-(json+(("id", JsString(UUID.randomUUID().toString)))))
//      }
//      case None => Future.successful(\/-(json+(("id", JsString(UUID.randomUUID().toString)))))
//    }
//  }*/
//
//  //Deprecated
//  /*def completeTime(json: JsObject): Future[Expect[JsObject]] = {
//    json.fields.filter(field => field._2.isInstanceOf[JsString] &&
//      (field._2.as[String].split(' ').head == "time_plus" |
//       field._2.as[String].split(' ').head == "time_minus" |
//       field._2.as[String].split(' ').head == "time_now")) match {
//      case Nil => Future.successful(\/-(json))
//      case fields =>
//        val newJs = JsObject(fields.map(f =>
//          f._1 -> JsString(getTime(f._2.as[String], f._2.as[String].split(' ').head))))
//        Future.successful(\/-(json++newJs))
//    }
//  }*/
//
//  //Deprecated
//  /*def getTime(jsValue: String, operation: String): String = {
//    operation match {
//      case "time_plus" => jsValue.split(' ')(1) match {
//        case "years" => DateTime.now.plusYears(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "months" => DateTime.now.plusMonths(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "weeks" => DateTime.now.plusWeeks(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "days" => DateTime.now.plusDays(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "hours" => DateTime.now.plusHours(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "minutes" => DateTime.now.plusMinutes(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "seconds" => DateTime.now.plusSeconds(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "millis" => DateTime.now.plusMillis(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case _ => DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//      }
//      case "time_minus" => jsValue.split(' ')(1) match {
//        case "years" => DateTime.now.minusYears(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "months" => DateTime.now.minusMonths(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "weeks" => DateTime.now.minusWeeks(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "days" => DateTime.now.minusDays(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "hours" => DateTime.now.minusHours(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "minutes" => DateTime.now.minusMinutes(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "seconds" => DateTime.now.minusSeconds(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case "millis" => DateTime.now.minusMillis(jsValue.split(' ').last.toInt).toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//        case _ => DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//      }
//      case "time_now" => DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ss'Z'")
//    }
//  }*/
//
//  //Unused (could be moved to utils)
//  def isUUID(value: String): Boolean = {
//    try{
//      UUID.fromString(value)
//      true
//    } catch{
//      case e:IllegalArgumentException => false
//    }
//  }
//
//  def jsonValidate[B](json: JsObject, in: Class[B], formatIn: Format[B]): Future[Expect[JsObject]] = {
//    try{
//      implicit val format = formatIn
//      val input:B = json.as[B]
//      Future.successful(\/-(json))
//    } catch {
//      case e:Exception =>
//        Logger.error(e.getMessage)
//        val fields = in.getDeclaredFields.map{f=> (f.getName, f.getName + " : " + f.getGenericType.getTypeName.split('.').last)}
//        val missing = fields.filter(f => e.getMessage.contains(f._1))
//        Future.successful(-\/(FailError(e.getMessage + "\nError while validating json. "+ "Missing fields are : \n" + missing.map{f=>f._2}.mkString("\n"))))
//    }
//  }
//
//  def jsonUpdateValidate[B](json: JsObject, in: Class[B], formatIn: Format[B]): Future[Expect[JsObject]] = {
//    val jsFields = json.fields.map{f=> f._1}
//    val inputFields = in.getDeclaredFields.map{f=>f.getName}
//    val fields = jsFields.intersect(inputFields)
//    fields match {
//      case Nil => Future.successful(-\/(FailError("Error, these fields do not exist or cannot be updated")))
//      case t => Future.successful(\/-(JsObject(json.fields.filter(f=> t.contains(f._1)))))
//    }
//  }
//
//  def buildAddRequest[T, A](entity: T, singleton: Class[A], tableName : String): (String, String) = {
//    val const = singleton.getDeclaredConstructors()(0)
//    const.setAccessible(true)
//    val obj = const.newInstance()
//    val getTableColumnNames = singleton.getDeclaredMethod("getTableColumns", classOf[String])
//    getTableColumnNames.setAccessible(true)
//    val fields = entity.getClass.getDeclaredFields
//    val params = fields.flatMap{field => getTableColumnNames.invoke(obj, field.getName).asInstanceOf[Option[String]]}
//    val values = fields.map{field => field.setAccessible(true)
//      (field.get(entity), field.getGenericType.getTypeName)}
//    Logger.debug(values.toSeq.toString)
//    val value = concatValue(values.toList)
//    Logger.debug(value)
//    val param = concatParam(params.toList)
//    (param, value)
//  }
//
//  def buildUpdateRequest[T, A](model: Class[T], json: JsObject, singleton: Class[A], tableName : String, format: Format[T]): String = {
//    val const = singleton.getDeclaredConstructors()(0)
//    const.setAccessible(true)
//    val obj = const.newInstance()
//    val getTableColumnNames = singleton.getDeclaredMethod("getTableColumns", classOf[String])
//    getTableColumnNames.setAccessible(true)
//    val fields = model.getDeclaredFields
//    implicit val form = format
//    val entity:T = json.as[T]
//    val params = fields.map{field => field.setAccessible(true)
//      (getTableColumnNames.invoke(obj, field.getName).asInstanceOf[Option[String]],
//        field.get(entity).toString,
//        field.getGenericType.getTypeName)}
//    Logger.debug(concatParamAndValue(params.toList))
//    concatParamAndValue(params.toList)
//  }
//
//  def concatParam(params: List[String], param: String = ""): String = {
//    params match {
//      case x::xs => concatParam(xs, param + x + ", ")
//      case Nil => param.splitAt(param.length -2)._1
//    }
//  }
//
//  def concatValue(values: List[(AnyRef, String)], value: String = ""): String = {
//    values match {
//      case x::xs => concatValue(xs, value + valueToAdd(x._1.toString, x._2))
//      case Nil => value.splitAt(value.length-2)._1
//    }
//  }
//
//  def concatParamAndValue(list: List[(Option[String], String, String)], value: String = ""): String = {
//    list match {
//      case x::xs => x match {
//        case (Some(name),v,t) if !(name == "id") =>
//          concatParamAndValue(xs, value + name + " = " + valueToAdd(v, t))
//        case _  => concatParamAndValue(xs, value)
//      }
//      case Nil => value.splitAt(value.length-2)._1
//    }
//  }
//
//  def valueToAdd(value: String, typeName: String): String = {
//    (value, typeName) match {
//      case (y, z) if z.contains("UUID") =>
//        "'" + innerValue(y, z) + "'::UUID, "
//      case (y,z) if z.contains("scala.Option") && !y.contains("Some") =>
//        "null, "
//      case (y,z) => "'" + innerValue(y, z) + "', "
//    }
//  }
//
//  def innerValue(value: String, typeName: String): String = {
//    //val r = """(?!^'|'$)(\')"""
//    val r = """(\')"""
//    val escapeSimpleQuote = r.r replaceAllIn (value, "''")
//    (escapeSimpleQuote, typeName) match {
//      case (y, z) if z.contains("scala.Option") && y.contains("Some") =>
//        y.splitAt(5)._2.splitAt(y.length-6)._1
//      case (y, z) if z.contains("List") =>
//        "{" + y.splitAt(5)._2.splitAt(y.length-6)._1 + "}"
//      case _ => escapeSimpleQuote
//    }
//  }
//
//  def getClassInfo[T, A, B](model: Class[T], singleton: Class[A], className: String, in: Class[B]): Future[Expect[(String, RowParser[T], Format[T], Format[B])]] = {
//    val const = singleton.getDeclaredConstructors()(0)
//    const.setAccessible(true)
//    val obj = const.newInstance()
//    val table = singleton.getDeclaredField("tableName")
//    table.setAccessible(true)
//    val parse = singleton.getDeclaredField("parse")
//    parse.setAccessible(true)
//    val jsFormat = singleton.getDeclaredField(className.split('.').last+"Format")
//    jsFormat.setAccessible(true)
//    val jsFormatIn = singleton.getDeclaredField(in.getName.split('.').last+"Format")
//    jsFormatIn.setAccessible(true)
//    val format = jsFormat.get(obj).asInstanceOf[Format[T]]
//    val formatIn = jsFormatIn.get(obj).asInstanceOf[Format[B]]
//    val parser = parse.get(obj).asInstanceOf[anorm.RowParser[T]]
//    val name = table.get(singleton.cast(obj)).asInstanceOf[String]
//    Future.successful(\/-((name, parser, format, formatIn)))
//  }
//
//
//  // TODO merge 2 functions
//
//  def toJsValue[T, A, C](newObject: Option[T], model: Class[T], singleton: Class[A], out: Class[C]): Future[Expect[JsValue]] = {
//    val const = singleton.getDeclaredConstructors()(0)
//    const.setAccessible(true)
//    val obj = const.newInstance()
//    val jsFormat = singleton.getDeclaredField(out.getName.split('.').last+"Format")
//    jsFormat.setAccessible(true)
//    implicit val format = jsFormat.get(obj).asInstanceOf[Format[C]]
//    val toOut: Method = singleton.getDeclaredMethod("to"+out.getName.split('.').last, model)
//    Future.successful(
//      \/-(
//        Json.toJson(
//          newObject
//            .map{ o =>
//              toOut
//                .invoke(obj, o.asInstanceOf[Object])
//                .asInstanceOf[C]
//            }
//        )
//      )
//    )
//  }
//
//  def toJsValue[T, A, C](newObject: List[T], model: Class[T], singleton: Class[A], out: Class[C]): Future[Expect[JsValue]] = {
//    val const = singleton.getDeclaredConstructors()(0)
//    const.setAccessible(true)
//    val obj = const.newInstance()
//    val jsFormat = singleton.getDeclaredField(out.getName.split('.').last+"Format")
//    jsFormat.setAccessible(true)
//    implicit val format = jsFormat.get(obj).asInstanceOf[Format[C]]
//    val toOut: Method = singleton.getDeclaredMethod("to"+out.getName.split('.').last, model)
//    Future.successful(
//      \/-(
//        Json.toJson(
//          newObject
//            .map{ o =>
//              toOut
//                .invoke(obj, o.asInstanceOf[Object])
//                .asInstanceOf[C]
//            }
//        )
//      )
//    )
//  }
//}