package com.noeupapp.middleware.crudauto

import java.lang.reflect.Method
import java.util.UUID
import javax.inject.Inject

import com.google.inject.TypeLiteral
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import org.joda.time.DateTime
import play.api.libs.json._
import com.noeupapp.middleware.utils.slick.{MyPostgresDriver, ResultMap}
import play.api.Logger
import slick.ast.Type

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._
import Scalaz._
import com.noeupapp.middleware.utils.slick.MyPostgresDriver.api._
import slick.lifted.{ForeignKey, TableQuery}
import play.api.mvc.Results._
import slick.profile.SqlStreamingAction
import slick.jdbc._
import com.noeupapp.middleware.utils.StringUtils.snakeToCamel

import scala.language.higherKinds
import scala.reflect.runtime.{universe => ru}

class CrudAutoService @Inject()(dao: Dao)() {



  def findAll[E <: Entity[Any]](tableQuery: TableQuery[Table[E] with PKTable],
                                search: Option[String],
                                countOnly: Boolean,
                                p: Option[Int], pp: Option[Int],
                                withDelete: Boolean)
                               (implicit formatE: Format[E]): Future[Expect[Seq[E]]] = {

    if (tableQuery.baseTableRow.create_*.map(_.name).toSeq.contains("deleted") && !withDelete)
      dao.runForAll(tableQuery.filter(row =>
        row.column[Boolean]("deleted") === false)) // TODO column("id") should be a generik pk
    else
      dao.runForAll(tableQuery) // TODO column("id") should be a generik pk


  }

  /* depracated */
  //def findAllPlainSql[E <: Entity[Any]](tableQuery: TableQuery[Table[E] with PKTable],
  //                              search: Option[String],
  //                              countOnly: Boolean, p: Option[Int], pp: Option[Int])
  //                             (implicit formatE: Format[E], eClass: ClassTag[E]): Future[Expect[Seq[E]]] = {
  //
  //  if (p.isDefined ^ pp.isDefined){
  //    return Future.successful(-\/(FailError("One of query param `p` or `pp` is missing")))
  //  }
  //
  //  val tableName = tableQuery.baseTableRow.tableName
  //  val cols = tableQuery.baseTableRow.create_*.map(_.name).toSeq
  //  val select = sql"SELECT * "
  //  //val select = if (countOnly)  sql"SELECT COUNT(*) " else sql"SELECT * " // TODO tries to implement count in sql but need to chnge Json validate
  //  val deleted = if (cols.contains("deleted")) sql"""WHERE #$tableName.deleted = FALSE """ else sql"WHERE TRUE"
  //
  //  val pagination = if (p.isDefined) sql""" LIMIT ${pp.get} OFFSET ${pp.get * p.get}""" else sql""
  //
  //  //def searchAllCols(tabl:TableQuery[Table[E] with PKTable], col: Map[String, Type], cond: String)/*: Iterable[Query[PostgresDriver.api.Table[E] with PKTable, E, Seq]]*/ =
  //    //cols(tableQuery)//.filter{case (n, t) => t.String}
  //    //                .map{case (n, t) => tableQuery//.filter(t.isInstanceOf[String])
  //    //                                              .filter[String](_.column(n) like cond)}
  //
  //    // if (cols(tableQuery).contains("deleted"))
  //    //   dao.runForAll(tableQuery.filter(_.column[Boolean]("deleted") === false)
  //    //                //           .map(s=> searchAllCols(s, cols, "molt clé"))
  //    //                )
  //    // else
  //    //   dao.runForAll(tableQuery)
  //
  //  //val q: SqlStreamingAction[Vector[Map[String, Any]], Map[String, Any], Effect] = {
  //  //  val req = select concat sql"""FROM #$tableName """ concat deleted
  //  //              .concat(filt(tableQuery, search).map(sql" AND " concat _).getOrElse(sql""))
  //  //              .concat(pagination)
  //  //  req.as(ResultMap)
  //  //}
  //  //
  //  //dao.runTransformer(q) { row => rowTransformer(row) }
  //
  //  //def mkGetResult[T](next: (PositionedResult => T)) =
  //  //  new GetResult[T] {
  //  //    def apply(rs: PositionedResult) = next(rs)
  //  //  }
  //
  //  //implicit val getTableResult = mkGetResult[E]()
  //  //implicit val getTableResult = GetResult[E](r => eClass.runtimeClass.asInstanceOf[Class[E]](r.<<, r.<<))
  //
  //  val req = select concat sql"""FROM #$tableName """ concat deleted
  //              .concat(filt(tableQuery, search).map(sql" AND " concat _).getOrElse(sql""))
  //              .concat(pagination)
  //  dao.runSqlStreamingAction(req.as[Seq[E]])
  //  //dao.run(req)
  //}

/*
  def deepFindAll[E <: Entity[Any], F <: Entity[Any], PKE](tableQuery: TableQuery[Table[F] with PKTable],
                                                           id: PKE,
                                                           joinedTable: ForeignKey//String
                                                          )(implicit formatE: Format[E]): Future[Expect[Seq[E]]] = {
    val joinedTableName = joinedTable.targetTable.tableName
    val sourceColumns = joinedTable.sourceColumns  // TODO Warning when multiple columns
    val targetColumns = /*joinedTableName + "." + */joinedTable.linearizedTargetColumns.head.getDumpInfo.mainInfo // TODO Warning when multiple columns
    val tableName = tableQuery.baseTableRow.tableName
    val typeId = findTypeToString(id)

    if (tableQuery.baseTableRow.create_*.map(_.name).toSeq.contains("deleted"))
      dao.runForAll(tableQuery.filter(_.column[Boolean]("deleted") === false))
    else
      dao.runForAll(
        (tableName join joinedTableName on (_.1/*targetColumns*/ === _.2/*sourceColumns*/ ))
        .map{ case (p, a) => _/*(p.name, a.city)*/ }
      )
  }
  */

  def deepFindAll[E <: Entity[Any], F <: Entity[Any], PKE](tableQuery: TableQuery[Table[F] with PKTable],
                                                       id: PKE,
                                                       joinedTable: ForeignKey,
                                                       search: Option[String],
                                                       p: Option[Int],
                                                       pp: Option[Int]
                                                      )(implicit formatE: Format[E]): Future[Expect[Seq[E]]] = {

    val joinedTableName = joinedTable.targetTable.tableName
    val sourceColumns = joinedTable.sourceColumns  // TODO Warning when multiple columns
    val targetColumns = joinedTableName + "." + joinedTable.linearizedTargetColumns.head.getDumpInfo.mainInfo // TODO Warning when multiple columns
    val tableName = tableQuery.baseTableRow.tableName
    val typeId = findTypeToString(id)
    val cols = tableQuery.baseTableRow.create_*.map(_.name).toSeq
    val select = sql"SELECT #$tableName.* "
    val deleted = if (cols.contains("deleted")) sql"""WHERE #$tableName.deleted = FALSE """ else sql"WHERE TRUE"
    val pagination = if (p.isDefined) sql""" LIMIT ${pp.get} OFFSET ${pp.get * p.get}""" else sql""


    val q: SqlStreamingAction[Vector[Map[String, Any]], Map[String, Any], Effect] = {
      val req = select concat sql"""FROM #$tableName
                                    INNER JOIN #$joinedTableName ON #$targetColumns = #$sourceColumns AND #$targetColumns = ${id.toString}::#${typeId}
                                 """ concat deleted
        .concat(filt(tableQuery, search).map(sql" AND " concat _).getOrElse(sql""))
        .concat(pagination)

      req.as(ResultMap)
    }

    dao.runTransformer(q) { row => rowTransformer(row) }
  }


  def deepFindById[E <: Entity[Any], F <: Entity[Any], PKE, PKF](tableQuery: TableQuery[Table[F] with PKTable],
                                                       id1: PKE,
                                                       joinedTable: ForeignKey,//String,
                                                       id2: PKE
                                                      )(implicit formatF: Format[F]): Future[Expect[Option[F]]] = {

    val joinedTableName = joinedTable.targetTable.tableName
    val sourceColumns = joinedTable.sourceColumns  // TODO Warning when multiple columns
    val targetColumns = joinedTableName + "." + joinedTable.linearizedTargetColumns.head.getDumpInfo.mainInfo // TODO Warning when multiple columns
    val tableName = tableQuery.baseTableRow.tableName
    val cols = tableQuery.baseTableRow.create_*.map(_.name).toSeq
    val select = sql"SELECT #$tableName.* "
    val deleted = if (cols.contains("deleted")) sql"""AND #$tableName.deleted = FALSE """ else sql""
    id1.isInstanceOf[UUID]
    val typeId1 = findTypeToString(id1)
    val typeId2 = findTypeToString(id2)

    val q: SqlStreamingAction[Vector[Map[String, Any]], Map[String, Any], Effect] = {
      val req = select concat sql"""FROM #$tableName
                                    INNER JOIN #$joinedTableName ON #$targetColumns = #$sourceColumns AND #$targetColumns = ${id1.toString}::#${typeId1}
                                    WHERE #$tableName.id = ${id2.toString}::#${typeId2}
                                   """ concat deleted
      req.as(ResultMap)
    }

    dao.runTransformer(q) { row => rowTransformer(row) }

  }.map(_.map(_.headOption))


  def find[E <: Entity[PK], PK](tableQuery: TableQuery[Table[E] with PKTable],
                                id: PK,
                                withDelete: Boolean)
                               (implicit bct: BaseColumnType[PK]): Future[Expect[Option[E]]] = {

    if (tableQuery.baseTableRow.create_*.map(_.name).toSeq.contains("deleted") && !withDelete)
//      dao.runForAll(tableQuery.filter(_.column[Boolean]("deleted") === false))
      dao.runForHeadOption(tableQuery.filter(row =>
        row.column[PK]("id") === id && row.column[Boolean]("deleted") === false)) // TODO column("id") should be a generik pk
    else
      dao.runForHeadOption(tableQuery.filter(_.column[PK]("id") === id)) // TODO column("id") should be a generik pk

  }


  def add[E <: Entity[Any], PK, V <: Table[E]]( tableQuery: TableQuery[V],
                                                entity: E)(implicit bct: BaseColumnType[PK]): Future[Expect[E]] = {
    // TODO column("id") should be a generik pk
    val insertQuery: MyPostgresDriver.IntoInsertActionComposer[E, E] = tableQuery returning tableQuery.map(_.column[PK]("id")) into { (e, id) =>
      e.withNewId(id = id).asInstanceOf[E] // TODO find a way to do that with out ugly cast
    }
    dao.run(insertQuery += entity)
  }


  def upsert[E <: Entity[Any], PK, V <: Table[E] with PKTable](tableQuery: TableQuery[V], entity: E)(implicit bct: BaseColumnType[PK]): Future[Expect[E]] =
    dao.run(tableQuery.insertOrUpdate(entity)).map(_.map(_ => entity))


  def update[E <: Entity[Any], PK, V <: Table[E]]( tableQuery: TableQuery[V with PKTable],
                                                   id: PK,
                                                   entity: E)(implicit bct: BaseColumnType[PK]): Future[Expect[E]] =
    dao.run(tableQuery.filter(_.column[PK]("id") === id).update(entity)).map(_.map(_ => entity)) // TODO column("id") should be a generik pk


  def delete[E <: Entity[Any], PK](tableQuery: TableQuery[Table[E] with PKTable],
                                   id : Any,//id: PK,
                                   force_delete: Boolean)(implicit bct: BaseColumnType[Any]): Future[Expect[Any]] = {
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




  /*
   *  UTILS
   */

  def rowTransformer[A](row: Vector[Map[String, Any]])(implicit formatE: Format[A]) =
    row.foldLeft[JsResult[List[A]]](JsSuccess(List.empty[A])){
      case (JsSuccess(els, _), e) => {

        val toSeq = e.map {
          case (k, v) => (snakeToCamel(k),
            v match {
              case v: Short => JsNumber(v.asInstanceOf[Long])
              case v: java.math.BigDecimal => JsNumber(v.asInstanceOf[java.math.BigDecimal])
              case v: Float => JsNumber(v.asInstanceOf[Double])
              case v: Double => JsNumber(v.asInstanceOf[Double])
              case v: Int => JsNumber(v.asInstanceOf[Int])
              case v: Long => JsNumber(v.asInstanceOf[Long])
              case v: DateTime => JsString(v.toString)
              case v: java.sql.Timestamp => JsNumber(v.getTime)
              case v: Boolean => JsBoolean(v)
              case None => JsNull
              case _ => JsString(v.toString)
            })
        }.toSeq


        JsObject(toSeq).validate[A] match {
          case JsSuccess(el, path) => JsSuccess(el :: els, path)
          case error@JsError(_) => error
        }
      }
      case (error @ JsError(_), _) => error
    }


  implicit class SQLActionBuilderConcat (a: SQLActionBuilder) {
    def concat (b: SQLActionBuilder): SQLActionBuilder =
      SQLActionBuilder(a.queryParts ++ b.queryParts, new SetParameter[Unit] {
        def apply(p: Unit, pp: PositionedParameters): Unit = {
          a.unitPConv.apply(p, pp)
          b.unitPConv.apply(p, pp)
        }
      })
  }

  def cols[E](tabl:TableQuery[Table[E] with PKTable]): Map[String, Type] =
    tabl.baseTableRow.create_*.map(c=> (c.name, c.tpe)).toMap

  def filt[E](tabl:TableQuery[Table[E] with PKTable], search: Option[String]): Option[SQLActionBuilder] = {
    search.filter(_.nonEmpty).map(s => {
      val sPercent = s"%$s%"
      val c = cols(tabl).map( x => {
        val sTable = s"${tabl.baseTableRow.tableName}.${x._1}"
        sql""" LOWER (#$sTable::TEXT) LIKE LOWER($sPercent)"""
      }
      ).reduceLeft[SQLActionBuilder]{
        case (acc, e) => acc concat sql" OR " concat e
      }
      //Logger.debug("filt x : " + c)
      c
    })}

  private def findTypeToString(id: Any) = id match {
    case _:UUID => "UUID"
    case _:Long => "bigint"
    case _:Int  => "int"
    case _      => "TEXT"
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

  def findAll(search: Option[String] = None, count: Option[Boolean] = Some(false), withDeleted: Boolean = false)(implicit bct: BaseColumnType[PK], formatE: Format[E], p: Option[Int] = None, pp: Option[Int] = None): Future[Expect[Seq[E]]] =
    // for deprecated findAllPlainSql
    // crudAutoService.findAll[Entity[Any]](tableQueryAny, search, count.getOrElse(false), p, pp)(formatE.asInstanceOf[Format[Entity[Any]]]).map(_.map(_.map(_.asInstanceOf[E])))
    crudAutoService.findAll[Entity[Any]](tableQueryAny, search, count.getOrElse(false), p, pp, withDeleted)(formatE.asInstanceOf[Format[Entity[Any]]]).map(_.map(_.map(_.asInstanceOf[E])))
//=======
//  def findAll(search: Option[String] = None, count: Option[Boolean] = Some(false))(implicit bct: BaseColumnType[PK], formatE: Format[E]): Future[Expect[Seq[E]]] =
//    crudAutoService.findAll[Entity[Any]](tableQueryAny, search, count.getOrElse(false))(formatE.asInstanceOf[Format[Entity[Any]]]).map(_.map(_.map(_.asInstanceOf[E])))
//>>>>>>> Stashed changes

//  def deepFindAll[F <: Entity[Any], PKE](id: PKE, joinedTableName: String)(implicit formatF: Format[F]): Future[Expect[Seq[F]]] = ???
//
//
//  def deepFindById[F <: Entity[Any], PKE, PKF](id1: PKE,
//                                               joinedTableName: String,
//                                               id2: PKE
//                                              )(implicit formatF: Format[F]): Future[Expect[Option[F]]] = ???

  def find(id: PK, withDeleted: Boolean = false)(implicit bct: BaseColumnType[PK]): Future[Expect[Option[E]]] =
    crudAutoService.find[Entity[PK], PK](
      tableQuery.asInstanceOf[TableQuery[Table[Entity[PK]] with PKTable]]/*Any*/,
      id,
      withDeleted
    )(bct.asInstanceOf[BaseColumnType[PK]])
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