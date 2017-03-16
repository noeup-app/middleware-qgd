package com.noeupapp.middleware.crudauto

import java.lang.reflect.Method

import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.libs.json.{Format, JsValue, Json, Writes}

import scala.concurrent.Future
import scala.language.higherKinds
import scalaz.{Functor, \/-}
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._
import Scalaz._

/**
  * Created by damien on 16/03/2017.
  */
class JsonConverter {


  def toJsValue[T, A, C](newObject: T, model: Class[T], singleton: Class[A], out: Class[C], withDelete: Boolean): Future[Expect[JsValue]] =
    Future(\/-(
      toJsValueSimple(newObject, model, singleton, out, withDelete)
    ))


  private def toJsValueSimple[T, A, C](newObject: T, model: Class[T], singleton: Class[A], out: Class[C], withDelete: Boolean): JsValue = {
    val const = singleton.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val jsFormat = singleton.getDeclaredField(out.getName.split('.').last + "Format")
    jsFormat.setAccessible(true)

    implicit val format = jsFormat.get(obj).asInstanceOf[Format[C]]

    val toJson: C =
      if (withDelete) newObject.asInstanceOf[C]
      else {
        val toOut: Method = singleton.getDeclaredMethod("to" + out.getName.split('.').last, model)
        toOut
          .invoke(obj, newObject.asInstanceOf[Object])
          .asInstanceOf[C]
      }

    Json.toJson(toJson)(format)
  }

  def toJsValueOpt[T, A, C](newObject: Option[T], model: Class[T], singleton: Class[A], out: Class[C], withDelete: Boolean): Future[Expect[Option[JsValue]]] = {

    if (newObject.isEmpty)
      return Future.successful(\/-(None))

    toJsValueGeneric[Option, T, A, C](
      newObject,
      model,
      singleton,
      out,
      withDelete,
      Writes.OptionWrites
    ).map(_.map(Option(_)))
  }

  def toJsValueList[T, A, C](newObject: List[T], model: Class[_], singleton: Class[A], out: Class[C], withDelete: Boolean): Future[Expect[JsValue]] = {
    toJsValueGeneric[List, T, A, C](
      newObject,
      model,
      singleton,
      out,
      withDelete,
      Writes.traversableWrites
    )
  }

  def toJsValueGeneric[F[_], T, A, C](newObject: F[T],
                                      model: Class[_],
                                      singleton: Class[A],
                                      out: Class[C],
                                      withDelete: Boolean,
                                      tWrites: Writes[F[JsValue]])
                                     (implicit fun: Functor[F]): Future[Expect[JsValue]] = {

    lazy val toJson = fun.map(newObject)(toJsValueSimple[T, A, C]
      (_, model.asInstanceOf[Class[T]], singleton, out, withDelete))

    Future(\/-(Json.toJson(toJson)(tWrites)))
  }

}
