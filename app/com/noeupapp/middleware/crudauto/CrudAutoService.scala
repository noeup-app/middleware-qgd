package com.noeupapp.middleware.crudauto

import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.crudauto.CrudAuto._
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.utils.StringUtils._
import org.joda.time.DateTime
import play.api.libs.json.JsObject

import scala.concurrent.Future
import scalaz._

class CrudAutoService  @Inject()(crudAutoDAO: CrudAutoDAO)() {

  def getClassName(model: String): Future[Expect[String]] = {

    CrudAuto.supportedClasses.find(name => name.equalsIgnoreCase(toSingular(model))) match {
      case Some(className) => Future.successful(\/-(className))
      case None => Future.successful(-\/(FailError("this model is not supported")))
    }
  }

  def findById(className: String, id: UUID): Future[Expect[Option[JsObject]]] = ???
}