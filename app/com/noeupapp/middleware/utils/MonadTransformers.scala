package com.noeupapp.middleware.utils

import com.noeupapp.middleware.errorHandle.FailError._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Logger

import scala.annotation.tailrec
import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

object MonadTransformers {
  @tailrec
  final def expectList2ListExpect[T](elements: List[Expect[T]], buffer: List[T] = List.empty): Expect[List[T]] = elements match {
    case element :: tail  =>

      element match {
        case \/-(res) =>
          expectList2ListExpect(tail, res :: buffer)
        case -\/(error) => -\/(error)
      }
    case Nil                =>
      \/-(buffer)
  }


  def listMonadTransformation[T, U](list: List[T], function: T => Future[Expect[U]], identity: U): Future[Expect[U]] = list match {
    case x :: xs  =>
      function(x) flatMap {
        case \/-(_) => listMonadTransformation(xs, function, identity)
        case -\/(e) => Future {
          -\/(e)
        }
      }
    case Nil      => Future {
      \/-(identity)
    }
  }


  def convertListEitherToListTuple[A, E](l: List[E\/A]): (List[E], List[A]) = {
    val r: (List[E\/A], List[E\/A]) =
      l.partition(_.isLeft)
    val left: List[E] = r._1
      .map(_.toEither)
      .foldLeft(List.empty[E]) ((acc, e) => acc ++ List(e.left.get))
    val right: List[A] = r._2
      .map(_.toEither)
      .foldLeft(List.empty[A])((acc, e) => acc ++ List(e.right.get))
    (left, right)
  }
}
