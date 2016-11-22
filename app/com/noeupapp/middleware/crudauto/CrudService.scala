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

  def findByIdFlow(model:String, id: UUID): Future[Expect[JsValue]] =
    {
      for {
        name <- EitherT(getClassName(model))
        entityClass = name.entityClass
        pkClass = name.pK
        tableDefClass = name.tableDef
        singleton = Class.forName(name.entityClass.getName + "$")
        //
        //        input = Class.forName(name+"In")
        out = Class.forName(name.entityClass.getName + "Out")

        tableQuery = TableQuery(tag =>
          tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
            .newInstance(tag)
            .asInstanceOf[Table[Entity] with PKTable[Object]])
        //.asInstanceOf[slick.lifted.AbstractTable[?]])
        //        tableDefClass.getConstructor(classOf[Tag]).newInstance(tag))

        found <- EitherT(
          crudAutoService.find(tableQuery, id)
          (name.baseColumnType.asInstanceOf[BaseColumnType[Object]])
        )
        newJson <- EitherT(crudAutoService.toJsValue(found.toList, entityClass, singleton, out))
      } yield newJson
    }.run


  def findAllFlow(model:String): Future[Expect[JsValue]] =
    {
      for {
        name <- EitherT(getClassName(model))
        entityClass = name.entityClass
        pkClass = name.pK
        tableDefClass = name.tableDef
        singleton = Class.forName(name.entityClass.getName + "$")
        //
        //        input = Class.forName(name+"In")
        out = Class.forName(name.entityClass.getName + "Out")

        tableQuery = TableQuery(tag =>
          tableDefClass.asInstanceOf[Class[_]].getConstructor(classOf[Tag])
            .newInstance(tag)
            .asInstanceOf[Table[Entity] with PKTable[BaseColumnType[_]]])
        //.asInstanceOf[slick.lifted.AbstractTable[?]])
        //        tableDefClass.getConstructor(classOf[Tag]).newInstance(tag))

        found <- EitherT(crudAutoService.findAll(tableQuery))
        newJson <- EitherT(crudAutoService.toJsValue(found.toList, entityClass, singleton, out))
      } yield newJson
    }.run

  protected def getClassName(model: String): Future[Expect[CrudConfigurationUnTyped]]

}


class CrudService @Inject()(val crudAutoService: CrudAutoService,
                            crudClassName: CrudClassName) extends AbstractCrudService{

  override def getClassName(model: String): Future[Expect[CrudConfigurationUnTyped]] = {
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

