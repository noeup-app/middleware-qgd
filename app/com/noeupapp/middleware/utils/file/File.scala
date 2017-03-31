package com.noeupapp.middleware.utils.file

import java.io.FileInputStream
import java.security.{DigestInputStream, MessageDigest}

import com.noeupapp.middleware.errorHandle.FailError.Expect

import scala.concurrent.Future
import scalaz.\/-
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by damien on 30/03/2017.
  */
object File {

  implicit class MyFile(file: java.io.File) {
    def hash: Future[Expect[String]] = Future {
      val buffer = new Array[Byte](8192)
      val md5 = MessageDigest.getInstance("MD5")

      val dis = new DigestInputStream(new FileInputStream(file), md5)
      try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

      \/-(md5.digest.map("%02x".format(_)).mkString)
    }
  }

}
