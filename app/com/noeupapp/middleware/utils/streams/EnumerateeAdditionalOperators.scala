package com.noeupapp.middleware.utils.streams

import play.api.libs.iteratee.{Enumeratee, Enumerator, Input, Iteratee, Traversable}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global


object EnumerateeAdditionalOperators {
  implicit def enumerateeAdditionalOperators(e: Enumeratee.type): EnumerateeAdditionalOperators = new EnumerateeAdditionalOperators(e)
}


/**
  * https://github.com/michaelahlers/michaelahlers-playful/blob/master/src/main/scala/ahlers/michael/playful/iteratee/EnumerateeFactoryOps.scala
  */
class EnumerateeAdditionalOperators(e: Enumeratee.type) {

  /**
    * As a complement to [[play.api.libs.iteratee.Enumeratee.heading]] and [[play.api.libs.iteratee.Enumeratee.trailing]], allows for inclusion of arbitrary elements between those from the producer.
    */
  def joining[E](separators: Enumerator[E])(implicit ec: ExecutionContext): Enumeratee[E, E] =
    zipWithIndex[E] compose Enumeratee.mapInputFlatten[(E, Int)] {

      case Input.Empty =>
        Enumerator.enumInput[E](Input.Empty)

      case Input.El((element, index)) if 0 < index =>
        separators andThen Enumerator(element)

      case Input.El((element, _)) =>
        Enumerator(element)

      case Input.EOF =>
        Enumerator.enumInput[E](Input.EOF)

    }

  /**
    * Zips elements with an index of the given [[scala.math.Numeric]] type, stepped by the given function.
    *
    * (Special thanks to [[https://github.com/eecolor EECOLOR]] for inspiring this factory with his answer to [[http://stackoverflow.com/a/27589990/700420 a question about enumeratees on Stack Overflow]].)
    */
  def zipWithIndex[E, I](first: I, step: I => I)(implicit ev: Numeric[I]): Enumeratee[E, (E, I)] =
    e.scanLeft[E](null.asInstanceOf[E] -> ev.minus(first, step(ev.zero))) {
      case ((_, index), value) =>
        value -> step(index)
    }

  /**
    * Zips elements with an incrementing index of the given [[scala.math.Numeric]] type, adding one each time.
    */
  def zipWithIndex[E, I](first: I)(implicit ev: Numeric[I]): Enumeratee[E, (E, I)] = zipWithIndex(first, ev.plus(_, ev.one))

  /**
    * Zips elements with an incrementing index by the same contract [[scala.collection.GenIterableLike#zipWithIndex zipWithIndex]].
    */
  def zipWithIndex[E]: Enumeratee[E, (E, Int)] = zipWithIndex(0)






  def splitToLines: Enumeratee[String, String] = e.grouped(
    Traversable.splitOnceAt[String,Char](_ != '\n')  &>>
      Iteratee.consume()
  )


}
