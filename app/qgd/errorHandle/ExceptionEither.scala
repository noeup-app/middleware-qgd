package qgd.errorHandle

import play.api.mvc.Results._
import play.api.mvc._
import qgd.errorHandle.FailError.Expect
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._

object ExceptionEither {

  def Try[T](t: => T): Expect[T] = {
    try{
      \/-(t)
    }catch {
      case e: Exception => -\/(FailError(e.getMessage, Some(-\/(e))))
    }
  }

  def TryToOption[T](t: => T): Option[T] = {
    try{
      Some(t)
    }catch {
      case e: Exception => None
    }
  }

  def FTry[T](t: => T): Future[Expect[T]] = {
    try{
      Future{\/-(t)}
    }catch {
      case e: Exception => Future{-\/(FailError(e.getMessage, Some(-\/(e))))}
    }
  }

  def FRTry[T](t: => T): Future[Result \/ T] = try{Future(\/-(t))} catch {case e: Exception => Future{-\/(InternalServerError(e.getMessage))}}

  def RTry[T](t: => T): (Result \/ T) = try{\/-(t)} catch {case e: Exception => -\/(InternalServerError(e.getMessage))}

}
