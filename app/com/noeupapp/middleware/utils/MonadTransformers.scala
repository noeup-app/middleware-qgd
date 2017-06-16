package com.noeupapp.middleware.utils

import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.annotation.tailrec
import scala.collection.{IterableLike, immutable}
import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/, \/-}

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


  /**
    * Execute sequentially a function on each element of a list
    *
    * @param list
    * @param function
    * @param identity
    * @tparam T
    * @tparam U
    * @return
    */
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


  def mapEitherList[Input, Output](list: List[Input], f: Input => Future[Expect[Output]]): Future[Expect[List[Output]]] = {
    import scalaz._
    import scalaz.Scalaz._
    import Scalaz._

    list
      .map(e => EitherT(f(e)))
      .sequenceU
      .run
  }


  /**
    * Map functions over a list of element and return errors and successes
    */
  def parallelListProcessing[Input, Output](list: List[Input],
                                            function: Input => Future[FailError\/Output]
                                            ): Future[(List[(Input, FailError)], List[(Input, Output)])] = {

      Future.sequence{
        list
          .map(e => function(e).map((e, _)))
      }.map(_.foldLeft((List.empty[(Input, FailError)], List.empty[(Input, Output)])){
        case ((fails, successes), (e, res)) if res.isLeft =>
          ((e, res.toEither.left.get) :: fails, successes)
        case ((fails, successes), (e, res)) if res.isRight =>
          (fails, (e, res.toEither.right.get) :: successes)
      })

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


  implicit class MyTraversableOnce[+A](it: TraversableOnce[A]){

    private val l: List[A] = it.toList

    def foldLeftFutureExpect[B, C](z: B)(op: (B, A) => Future[Expect[C]])(op2: (B, C) => B): Future[Expect[B]] = {
      def f(xs:  List[A], acc: B): Future[Expect[B]] = xs match {
        case Nil => Future.successful(\/-(acc))
        case x :: xss =>
          op(acc, x).flatMap{
            case \/-(success) => f(xss, op2(acc, success))
            case error @ -\/(_) => Future.successful(error)
          }
      }
      f(l, z)
    }

  }

}
