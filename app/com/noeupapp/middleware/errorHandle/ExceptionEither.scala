package com.noeupapp.middleware.errorHandle

import java.sql.Connection

import play.api.mvc.Results._
import play.api.mvc._
import com.noeupapp.middleware.errorHandle.FailError.Expect
import play.api.Logger
import play.api.db.DB

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import scalaz._
import play.api.Play.current

object ExceptionEither {

  def TryBDCall[T](unsafeBlock: (Connection) => Expect[T]): Future[Expect[T]] = {
    scala.util.Try {
      DB.withTransaction({ implicit c =>
        unsafeBlock(c)
      })
    } match {
      case Failure(e) =>
        Future.successful(-\/(FailError(s"Error while TryDBCall : ${e.getMessage}", e)))
      case Success(res) => Future.successful(res)
    }
  }

  def Try[T](t: => T): Expect[T] = {
    try{
      \/-(t)
    }catch {
      case e: Exception => -\/(FailError(e.getMessage, Some(-\/(e))))
    }
  }

  def TryExpect[T](t: => Expect[T]): Expect[T] = {
    try{
      t
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
      Future(\/-(t))
    }catch {
      case e: Exception => Future.successful(-\/(FailError(e)))
    }
  }.recover{
    case e: Exception => -\/(FailError(e))
  }

  def FRTry[T](t: => T): Future[Result \/ T] = try{Future(\/-(t))} catch {case e: Exception => Future{-\/(InternalServerError(e.getMessage))}}

  def RTry[T](t: => T): (Result \/ T) = try{\/-(t)} catch {case e: Exception => -\/(InternalServerError(e.getMessage))}

}
