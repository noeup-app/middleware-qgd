package com.noeupapp.middleware.crudauto

import java.util.UUID
import javassist.bytecode.stackmap.TypeData.ClassName
import javax.inject.Inject

import com.noeupapp.middleware.entities.entity.EntityDAO
import com.noeupapp.middleware.entities.entity.Entity
import com.noeupapp.middleware.crudauto.CrudAuto._
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.utils.MonadTransformers._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect

import com.noeupapp.middleware.utils.StringUtils._
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsValue, Json}
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

      classe = Class.forName("com.noeupapp.middleware.entities.entity."+"Entity$")
      found <- EitherT(findById(classe, id))

      json <- EitherT(toJsValue(found, name))
    } yield json
  }.run

  def getClassName(model: String): Future[Expect[String]] = {
    CrudAuto.supportedClasses.find(name => name.equalsIgnoreCase(toSingular(model))) match {
      case Some(className) => Future.successful(\/-(className))
      case None => Future.successful(-\/(FailError("this model is not supported")))
    }
  }

  def findById[T](classe: T, id: UUID): Future[Expect[Option[T]]] = {
    TryBDCall{ implicit c=>
      val clas = Class.forName("com.noeupapp.middleware.entities.entity."+"Entity$")
      Logger.debug("class = " + classe.toString)
      Logger.debug(Entity.toString)
      Logger.debug(Class.forName("com.noeupapp.middleware.entities.entity."+"Entity").getDeclaredFields.toSeq.toString())
      Logger.debug(classe.getClass.getName)
      Logger.debug(clas.getDeclaredFields.toSeq.toString())
      Logger.debug(Entity.getClass.getName)
      val table = clas.asInstanceOf[Class[T]].getDeclaredField("tableName")
      table.setAccessible(true)
      val name =  table.get(Entity).asInstanceOf[String]
      Logger.debug(name)
    val newEntity = crudAutoDAO.findById(clas.asInstanceOf[Class[T]], id, "entity_entities").asInstanceOf[T]
    \/-(Some(newEntity))
    }
  }

  def toJsValue[T](newObject: T, className: String): Future[Expect[JsValue]] = {
    Future.successful(\/-(Json.toJson(newObject.toString)))
  }
}