package com.noeupapp.middleware.utils

import scala.concurrent.Future
import scalaz.Monad
import scala.concurrent.ExecutionContext.Implicits.global

object FutureFunctor {
  // Fix the bug of Functor in for
  // Found at : https://groups.google.com/forum/#!msg/paris-scala-user-group/GMQ6NTvCfV4/DC9mx28w4EMJ
  implicit def futureMonad: Monad[Future] = new Monad[Future]  {
    override def bind[A, B](fa: Future[A])(f: A => Future[B]) = fa flatMap f
    override def point[A](a: ⇒ A) = Future(a)
    override def map[A, B](fa: Future[A])(f: A => B) = fa map f
  }
}
