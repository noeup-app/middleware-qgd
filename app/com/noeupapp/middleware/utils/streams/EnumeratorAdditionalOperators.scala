package com.noeupapp.middleware.utils.streams

import java.io.File
import java.nio.charset.Charset

import play.api.libs.iteratee.Enumerator

import scala.language.implicitConversions

import scala.concurrent.ExecutionContext.Implicits.global


object EnumeratorAdditionalOperators {
  implicit def enumeratorAdditionalOperators(e: Enumerator.type): EnumeratorAdditionalOperators = new EnumeratorAdditionalOperators(e)
}


class EnumeratorAdditionalOperators(e: Enumerator.type) {

  def fromUTF8File(file: File, chunkSize: Int = 1024 * 8): Enumerator[String] =
    e.fromFile(file, chunkSize)
      .map(bytes => new String(bytes, Charset.forName("UTF-8")))

}
