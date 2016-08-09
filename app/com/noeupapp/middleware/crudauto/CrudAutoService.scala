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

      classe = Class.forName("com.noeupapp.middleware.entities.entity."+name+"$")
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

  def findById[T](model: T, id: UUID): Future[Expect[Option[T]]] = {
    TryBDCall{ implicit c=>
      val classe = model.asInstanceOf[Class[T]]
      val const = classe.getDeclaredConstructors()(0)
      const.setAccessible(true)
      val obj = const.newInstance()
      val table = classe.getDeclaredField("tableName")
      table.setAccessible(true)
      val name =  table.get(classe.cast(obj)).asInstanceOf[String]
    val newEntity = crudAutoDAO.findById(classe, id, name).asInstanceOf[T]
    \/-(Some(newEntity))
    }
  }

  def toJsValue[T](newObject: T, className: String): Future[Expect[JsValue]] = {
    Future.successful(\/-(Json.toJson(newObject.toString)))
  }
}