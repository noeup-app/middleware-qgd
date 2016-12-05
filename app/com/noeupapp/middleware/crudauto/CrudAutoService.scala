package com.noeupapp.middleware.crudauto

import java.lang.reflect.{Field, Method}
import java.util.UUID
import javax.inject.Inject

import anorm.RowParser
import com.google.inject.TypeLiteral
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.utils.MonadTransformers._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.TypeCustom._
import org.joda.time.DateTime
import play.api.libs.json._
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.utils.slick.ResultMap
import org.joda.time.format.DateTimeFormat
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds
import scalaz.{-\/, \/-}
//import scalaz._
//import Scalaz._
import slick.driver._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ForeignKey, TableQuery, Tag}
import play.api.mvc.Results._
import slick.profile.SqlStreamingAction
import slick.jdbc.{GetResult, PositionedResult}

import scala.reflect.runtime.{universe => ru}

class CrudAutoService @Inject()(dao: Dao)() {





  def findAll[E <: Entity[Any]](tableQuery: TableQuery[Table[E] with PKTable]): Future[Expect[Seq[E]]] = {
    if (tableQuery.baseTableRow.create_*.map(_.name).toSeq.contains("deleted"))
      dao.runForAll(tableQuery.filter(_.column[Boolean]("deleted") === false))
    else
      dao.runForAll(tableQuery)
  }

/*  def deepFindAll[E <: Entity, F <: Entity, PKE, PKF](tableQuery: TableQuery[Table[E] with PKTable],
                                                      tableQuery2: TableQuery[Table[F] with PKTable])
                                                     (implicit bct: BaseColumnType[Any]): Future[Expect[Map[PKE,Seq[F]]]] = {
    //if (tableQuery.baseTableRow.create_*.map(_.name).toSeq.contains("deleted"))
    //  dao.runForAll(tableQuery.filter(_.column[Boolean]("deleted") === false))
    //else
    val t1 = tableQuery.filter(_.column[Boolean]("deleted") === false)
    val t2 = tableQuery2.filter(_.column[Boolean]("deleted") === false)
    val q = (t1 join t2 on (_.pk.name === _.pk.name))
            .map{case (a, b) => (a, b)}
//    val q =
//      for {
//        (t1, t2) <- t1 join t2 on (_.pk.name === _.pk.name)
//      } yield (t1, t2)
    dao.runForAll(q)
        .flatMap(x => x)
        .flatMap(x => x.groupBy(_.1))
  }*/


  private def findTypeToString(id: Any) = id match {
    case t:UUID => "UUID"
    case t:Long => "bigint"
    case t:Int => "int"
    case _ => "TEXT"
  }


  def deepFindAll[E <: Entity[Any], F <: Entity[Any], PKE](tableQuery: TableQuery[Table[F] with PKTable],
                                                       id: PKE,
                                                       joinedTableName: String
                                                      )(implicit formatF: Format[F]): Future[Expect[Seq[F]]] = {


    val tableName = tableQuery.baseTableRow.tableName

    val typeId = findTypeToString(id)

    val q: SqlStreamingAction[Vector[Map[String, Any]], Map[String, Any], Effect] =
      if (tableQuery.baseTableRow.create_*.map(_.name).toSeq.contains("deleted")) {
        sql"""SELECT t.*
            FROM #$tableName t
            INNER JOIN #$joinedTableName tj ON tj.id = t.#$joinedTableName AND tj.id = ${id.toString}::#${typeId}
            WHERE t.deleted = FALSE"""
          .as(ResultMap)
      }else{
        sql"""SELECT t.*
            FROM #$tableName t
            INNER JOIN #$joinedTableName tj ON tj.id = t.#$joinedTableName AND tj.id = ${id.toString}::#${typeId}"""
          .as(ResultMap)
      }





    dao.runTransformer[Vector[Map[String, Any]], List[F], Map[String, Any], Effect](q) { row =>
      row.foldLeft[JsResult[List[F]]](JsSuccess(List.empty[F])){
        case (JsSuccess(els, _), e) =>

          val toSeq = e.map{
            case (k, v) => (k, Json.toJson(v.toString))
          }.toSeq

          JsObject(toSeq).validate[F] match {
            case JsSuccess(el, path) => JsSuccess(el :: els, path)
            case error @ JsError(_)  => error
          }
        case (error @ JsError(_), _) => error
      }
    }
  }


  def deepFindById[E <: Entity[Any], F <: Entity[Any], PKE, PKF](tableQuery: TableQuery[Table[F] with PKTable],
                                                       id1: PKE,
                                                       joinedTableName: String,
                                                       id2: PKE
                                                      )(implicit formatF: Format[F]): Future[Expect[Option[F]]] = {

    val tableName = tableQuery.baseTableRow.tableName
    id1.isInstanceOf[UUID]

    val typeId1 = findTypeToString(id1)
    val typeId2 = findTypeToString(id2)

    val q: SqlStreamingAction[Vector[Map[String, Any]], Map[String, Any], Effect] =
      if (tableQuery.baseTableRow.create_*.map(_.name).toSeq.contains("deleted")) {
        sql"""SELECT t.*
            FROM #$tableName t
            INNER JOIN #$joinedTableName tj ON tj.id = t.#$joinedTableName AND tj.id = ${id1.toString}::#${typeId1}
            WHERE t.deleted = FALSE AND t.id = ${id2.toString}::#${typeId2}"""
          .as(ResultMap)
      }else{
        sql"""SELECT t.*
            FROM #$tableName t
            INNER JOIN #$joinedTableName tj ON tj.id = t.#$joinedTableName AND tj.id = ${id1.toString}::#${typeId1}
            WHERE t.id = ${id2.toString}::#${typeId2}"""
          .as(ResultMap)
      }



    dao.runTransformer(q) { row =>
      row.foldLeft[JsResult[List[F]]](JsSuccess(List.empty[F])){
        case (JsSuccess(els, _), e) =>

          val toSeq = e.map{
            case (k, v) => (k, Json.toJson(v.toString))
          }.toSeq

          JsObject(toSeq).validate[F] match {
            case JsSuccess(el, path) => JsSuccess(el :: els, path)
            case error @ JsError(_)  => error
          }
        case (error @ JsError(_), _) => error
      }
    }
  }.map(_.map(_.headOption))


  def find[E <: Entity[Any]](tableQuery: TableQuery[Table[E] with PKTable], id: Any)(implicit bct: BaseColumnType[Any]): Future[Expect[Option[E]]] = {
    if (tableQuery.baseTableRow.create_*.map(_.name).toSeq.contains("deleted"))
//      dao.runForAll(tableQuery.filter(_.column[Boolean]("deleted") === false))
      dao.runForHeadOption(tableQuery.filter(row =>
        row.column[Any]("id") === id && row.column[Boolean]("deleted") === false)) // TODO column("id") should be a generik pk
    else
      dao.runForHeadOption(tableQuery.filter(_.column[Any]("id") === id)) // TODO column("id") should be a generik pk
  }


  def add[E <: Entity[Any], PK, V <: Table[E]](tableQuery: TableQuery[V],
                                          entity: E)
                                         (implicit bct: BaseColumnType[PK]): Future[Expect[E]] = {
    // TODO column("id") should be a generik pk
    val insertQuery: PostgresDriver.IntoInsertActionComposer[E, E] = tableQuery returning tableQuery.map(_.column[PK]("id")) into { (e, id) =>
      e.withNewId(id = id).asInstanceOf[E] // TODO find a way to do that with out ugly cast
    }
    dao.run(insertQuery += entity)
  }


  def upsert[E <: Entity[Any], PK, V <: Table[E] with PKTable](tableQuery: TableQuery[V], entity: E)(implicit bct: BaseColumnType[PK]): Future[Expect[E]] =
    dao.run(tableQuery.insertOrUpdate(entity)).map(_.map(_ => entity))


  def update[E <: Entity[Any], PK, V <: Table[E]](tableQuery: TableQuery[V with PKTable],
                                   id: PK,
                                   entity: E)
                                   (implicit bct: BaseColumnType[PK]): Future[Expect[E]] =
    dao.run(tableQuery.filter(_.column[PK]("id") === id).update(entity)).map(_.map(_ => entity)) // TODO column("id") should be a generik pk


  def delete[E <: Entity[Any], PK](tableQuery: TableQuery[Table[E] with PKTable],
                                 id : Any,//id: PK,
                                 force_delete: Boolean)
                             (implicit bct: BaseColumnType[Any]): Future[Expect[Any]] = {
    if (tableQuery.baseTableRow.create_*.map(_.name).toSeq.contains("deleted") && ! force_delete) {
      dao.run{
        tableQuery.filter(_.column[Any]("id") === id) // TODO column("id") should be a generik pk
          .map(_.column[Boolean]("deleted"))
            .update(true)
      }.map(_.map(_ => id))
    }else{
      dao.run{
        tableQuery.filter(_.column[Any]("id") === id).delete // TODO column("id") should be a generik pk
      }.map(_.map(_ => id))
    }
  }.recover{
    case e: Exception => -\/(FailError(e))
  }








  def completeAdd[T, A, B](model: Class[T], in: Class[B], singleton: Class[A], modelIn: B, format: Format[T], formatIn: Format[B]): Future[Expect[T]] = {
    implicit val form = format
    implicit val formIn  = formatIn
//    val input:B = json.as[B]
    val consts = model.getDeclaredConstructors
    val const = consts.find(r=> r.getParameterTypes.contains(in))
    Logger.debug(const.mkString)
    const match {
      case Some(init) => init.setAccessible(true)
        val entity: T = init.newInstance(modelIn.asInstanceOf[Object]).asInstanceOf[T]
        Future.successful(\/-(entity))
      case None => Future.successful(-\/(FailError("couldn't find constructor")))
    }
  }//.run

  def completeUpdate[T](entity: T, json: JsObject, format: Format[T]): Future[Expect[T]] = Future {
    implicit val reads = format

    val oldJson = Json.toJson(entity).as[JsObject]

    (oldJson ++ json).validate[T] match {
      case JsSuccess(value, _) => \/-(value)
      case JsError(errors)     => -\/(FailError(s"Unable to completeUpdate error : $errors"))
    }
  }

  def jsonValidate[B](json: JsObject, in: Class[B], formatIn: Format[B]): Future[Expect[B]] = Future {
    implicit val format = formatIn
    json.validate[B] match {
      case JsSuccess(value, _) => \/-(value)
      case JsError(errors) =>
        -\/(FailError(s"Unable to validate json. Errors : $errors", errorType = BadRequest))
    }
  }

  def jsonUpdateValidate[B](json: JsObject, in: Class[B], formatIn: Format[B]): Future[Expect[JsObject]] = {
    val jsFields = json.fields.map{f=> f._1}
    val inputFields = in.getDeclaredFields.map{f=>f.getName}
    val fields = jsFields.intersect(inputFields)
    if(jsFields.diff(inputFields).nonEmpty)
      return Future.successful(-\/(FailError("Unexpected field in json input", errorType = BadRequest)))

    fields match {
      case Nil => Future.successful(-\/(FailError("Error, these fields do not exist or cannot be updated", errorType = BadRequest)))
      case t => Future.successful(\/-(JsObject(json.fields.filter(f=> t.contains(f._1)))))
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

  def buildUpdateRequest[T, A](model: Class[T], json: JsObject, singleton: Class[A], tableName : String, format: Format[T]): String = {
    val const = singleton.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val getTableColumnNames = singleton.getDeclaredMethod("getTableColumns", classOf[String])
    getTableColumnNames.setAccessible(true)
    val fields = model.getDeclaredFields
    implicit val form = format
    val entity:T = json.as[T]
    val params = fields.map{field => field.setAccessible(true)
      (getTableColumnNames.invoke(obj, field.getName).asInstanceOf[Option[String]],
        field.get(entity).toString,
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

  def concatParamAndValue(list: List[(Option[String], String, String)], value: String = ""): String = {
    list match {
      case x::xs => x match {
        case (Some(name),v,t) if !(name == "id") =>
          concatParamAndValue(xs, value + name + " = " + valueToAdd(v, t))
        case _  => concatParamAndValue(xs, value)
      }
      case Nil => value.splitAt(value.length-2)._1
    }
  }

  def valueToAdd(value: String, typeName: String): String = {
    (value, typeName) match {
      case (y, z) if z.contains("UUID") =>
        "'" + innerValue(y, z) + "'::UUID, "
      case (y,z) if z.contains("scala.Option") && !y.contains("Some") =>
        "null, "
      case (y,z) => "'" + innerValue(y, z) + "', "
    }
  }

  def innerValue(value: String, typeName: String): String = {
    //val r = """(?!^'|'$)(\')"""
    val r = """(\')"""
    val escapeSimpleQuote = r.r replaceAllIn (value, "''")
    (escapeSimpleQuote, typeName) match {
      case (y, z) if z.contains("scala.Option") && y.contains("Some") =>
        y.splitAt(5)._2.splitAt(y.length-6)._1
      case (y, z) if z.contains("List") =>
        "{" + y.splitAt(5)._2.splitAt(y.length-6)._1 + "}"
      case _ => escapeSimpleQuote
    }
  }


  case class ClassInfo[T, B](jsonFormat: Format[T], jsonInFormat: Format[B])

  def getClassInfo[T, A, B](model: Class[T], singleton: Class[A], className: String, in: Class[B]): Future[Expect[ClassInfo[T, B]]] = {
    val const = singleton.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val jsFormat = singleton.getDeclaredField(className.split('.').last+"Format")
    jsFormat.setAccessible(true)
    val jsFormatIn = singleton.getDeclaredField(in.getName.split('.').last+"Format")
    jsFormatIn.setAccessible(true)
    val format = jsFormat.get(obj).asInstanceOf[Format[T]]
    val formatIn = jsFormatIn.get(obj).asInstanceOf[Format[B]]
    Future.successful(\/-(ClassInfo(format, formatIn)))
  }



  def filterOmitsAndRequiredFieldsOfJsValue(jsValue: Option[JsValue], omits: List[String], require: List[String]): Future[Expect[Option[JsValue]]] =
    jsValue match {
      case None     => Future.successful(\/-(None))
      case Some(js) =>
        filterOmitsAndRequiredFieldsOfJsValue(js, omits, require).map(_.map(Some(_)))
    }

  def filterOmitsAndRequiredFieldsOfJsValue(jsValue: JsValue, omits: List[String], require: List[String]): Future[Expect[JsValue]] = Future {

    val filter: (JsObject) => JsObject =
      filterOmitFieldsOfJsValue(omits)_ andThen filterRequiredFieldsOfJsValue(require)

    jsValue match {
      case JsArray(elements) =>
        \/-(
          Json.toJson(
            elements.map{
              case obj: JsObject => filter(obj)
              case res => res
            }
          )
        )
      case obj: JsObject => \/-(filter(obj))
      case others => \/-(others)
    }
  }

  private def filterOmitFieldsOfJsValue(omits: List[String])(jsObj: JsObject): JsObject =
    omits.foldLeft[JsObject](jsObj)(_ - _)

  private def filterRequiredFieldsOfJsValue(requires: List[String])(jsObj: JsObject): JsObject =
    requires match {
      case Nil => jsObj
      case _ =>
        requires.foldLeft(Json.obj()) { (acc, e) =>
          jsObj.fieldSet.find(_._1 == e).map(acc + _).getOrElse(acc)
        }
    }


  // TODO merge 2 functions

  def toJsValueOpt[T, A, C](newObject: Option[T], model: Class[T], singleton: Class[A], out: Class[C]): Future[Expect[Option[JsValue]]] = {

    if (newObject.isEmpty)
      return Future.successful(\/-(None))

    val const = singleton.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val jsFormat = singleton.getDeclaredField(out.getName.split('.').last+"Format")
    jsFormat.setAccessible(true)
    implicit val format = jsFormat.get(obj).asInstanceOf[Format[C]]
    val toOut: Method = singleton.getDeclaredMethod("to"+out.getName.split('.').last, model)
    Future.successful(
      \/-(
        Some(
          Json.toJson(
            newObject
              .map{ o =>
                toOut
                  .invoke(obj, o.asInstanceOf[Object])
                  .asInstanceOf[C]
              }
          )
        )
      )
    )
  }

  def toJsValue[T, A, C](newObject: T, model: Class[T], singleton: Class[A], out: Class[C]): Future[Expect[JsValue]] = {
    val const = singleton.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val jsFormat = singleton.getDeclaredField(out.getName.split('.').last + "Format")
    jsFormat.setAccessible(true)
    implicit val format = jsFormat.get(obj).asInstanceOf[Format[C]]
    val toOut: Method = singleton.getDeclaredMethod("to" + out.getName.split('.').last, model)
    Future.successful(
      \/-(
        Json.toJson(

          toOut
            .invoke(obj, newObject.asInstanceOf[Object])
            .asInstanceOf[C]
        )
      )
    )
  }

  def toJsValueList[T, A, C](newObject: List[T], model: Class[_], singleton: Class[A], out: Class[C]): Future[Expect[JsValue]] = {
    val const = singleton.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val jsFormat = singleton.getDeclaredField(out.getName.split('.').last+"Format")
    jsFormat.setAccessible(true)
    implicit val format = jsFormat.get(obj).asInstanceOf[Format[C]]
    val toOut: Method = singleton.getDeclaredMethod("to"+out.getName.split('.').last, model)
    Future.successful(
      \/-(
        Json.toJson(
          newObject
            .map{ o =>
              toOut
                .invoke(obj, o.asInstanceOf[Object])
                .asInstanceOf[C]
            }
        )
      )
    )
  }
}


class CrudAutoFactory[E <: Entity[PK], PK] @Inject()( crudClassName: CrudClassName,
                                                      crudAutoService: CrudAutoService,
                                                      abstractCrudService: AbstractCrudService)
                                                      (implicit val eTypeLit: TypeLiteral[E],
                                                       val pkeTypeLit: TypeLiteral[PK]) {


  private val configurationOpt = crudClassName.configure.values.find(_.entityClass.getName equals eTypeLit.getRawType.getName)

  assert(configurationOpt.isDefined, s"CrudAutoFactory - Unable to find ${eTypeLit.getType.getTypeName} key in configuration")

  private val configuration = configurationOpt.get

  private val tableQuery: TableQuery[Table[E] with PKTable] =
    abstractCrudService.provideTableQuery(configuration.tableDef)
      .asInstanceOf[TableQuery[Table[E] with PKTable]]


  private val tableQueryAny: TableQuery[Table[Entity[Any]] with PKTable] =
    abstractCrudService.provideTableQuery(configuration.tableDef)
      .asInstanceOf[TableQuery[Table[Entity[Any]] with PKTable]]


//  type PK =

   val pkType: ru.Type = getType(configuration.pK)

  private def getType[T](clazz: Class[T]): ru.Type = {
    val runtimeMirror =  ru.runtimeMirror(clazz.getClassLoader)
    runtimeMirror.classSymbol(clazz).toType
  }

  def findAll: Future[Expect[Seq[E]]] =
    crudAutoService.findAll[Entity[Any]](tableQueryAny).map(_.map(_.map(_.asInstanceOf[E])))

//  def deepFindAll[F <: Entity[Any], PKE](id: PKE, joinedTableName: String)(implicit formatF: Format[F]): Future[Expect[Seq[F]]] = ???
//
//
//  def deepFindById[F <: Entity[Any], PKE, PKF](id1: PKE,
//                                               joinedTableName: String,
//                                               id2: PKE
//                                              )(implicit formatF: Format[F]): Future[Expect[Option[F]]] = ???

  def find(id: PK)(implicit bct: BaseColumnType[PK]): Future[Expect[Option[E]]] =
    crudAutoService.find[Entity[Any]](tableQueryAny, id)(bct.asInstanceOf[BaseColumnType[Any]])
      .map(_.map(_.map(_.asInstanceOf[E])))


  def add(entity: E)(implicit bct: BaseColumnType[PK]): Future[Expect[E]] =
    crudAutoService.add[Entity[Any], PK, Table[Entity[Any]] with PKTable](
      tableQueryAny,
      entity.asInstanceOf[Entity[Any]]
    ).map(_.map(_.asInstanceOf[E]))

  def upsert(entity: E)(implicit bct: BaseColumnType[PK]): Future[Expect[E]] =
    crudAutoService.upsert[Entity[Any], PK, Table[Entity[Any]] with PKTable](
      tableQueryAny,
      entity.asInstanceOf[Entity[Any]]
    )(bct).map(_.map(_.asInstanceOf[E]))

  def update(id: PK, entity: E)(implicit bct: BaseColumnType[PK]): Future[Expect[E]] =
    crudAutoService.update[Entity[Any], PK, Table[Entity[Any]]](
      tableQueryAny,
      id,
      entity.asInstanceOf[Entity[Any]]
    ).map(_.map(_.asInstanceOf[E]))

  def delete(id : PK, force_delete: Boolean)(implicit bct: BaseColumnType[PK]): Future[Expect[PK]] =
    crudAutoService.delete[Entity[Any], PK](
      tableQueryAny,
      id,
      force_delete
    )(bct.asInstanceOf[BaseColumnType[Any]]).map(_.map(_.asInstanceOf[PK]))



}