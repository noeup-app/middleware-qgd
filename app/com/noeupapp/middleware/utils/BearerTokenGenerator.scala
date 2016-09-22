package com.noeupapp.middleware.utils

import java.security.{MessageDigest, SecureRandom}
import java.util.Date
import javax.xml.bind.DatatypeConverter

import scala.util.Random

/**
 * Generates a Bearer Token with the length of 32 characters according to the
 * specification RFC6750 (http://http://tools.ietf.org/html/rfc6750)
 *
 * http://auconsil.blogspot.de/2013/06/create-bearer-token-in-scala.html
 */
object BearerTokenGenerator {

  val TOKEN_LENGTH = 32
  val TOKEN_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  val TOKEN_CHARS_REGEX = """[0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz\-._]+""".r
  val secureRandom = new SecureRandom()

  def generateToken: String =
    generateToken(TOKEN_LENGTH)

  def generateToken(tokenLength: Int): String =
    if (tokenLength == 0) ""
    else TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length())) +
      generateToken(tokenLength - 1)


  def isToken(str: String): Boolean =
    TOKEN_CHARS_REGEX.unapplySeq(str).isDefined
}

object UUIDGenerator {         // TODO MUCH TODO ABOUT NOTHING
  def uuid(): String = java.util.UUID.randomUUID.toString

}


/**
  * Generate an Auth Code by creating a SHA-1 digest of current date,
  *  and random string of length 100 characters.
  * @return Hex encoded SHA-1 digest.
  */
object AuthCodeGenerator {

  val CODE_LENGTH = 100

  def generateAuthCode(): String = {
    val md = MessageDigest.getInstance("SHA-1")
    val date = new Date
    val randomString = Random.nextString(CODE_LENGTH)
    md.update(date.toString().getBytes())
    md.update(randomString.getBytes())
    DatatypeConverter.printHexBinary(md.digest)
  }
}
