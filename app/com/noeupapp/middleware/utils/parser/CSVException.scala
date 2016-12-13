package com.noeupapp.middleware.utils.parser

/**
  * Exception to throw if something goes wrong during CSV parsing
  */
case class CSVException(s: String, line: Option[Line]) extends RuntimeException(s) {
  override def toString: String = s"CSVException(cause = $s, line = $line)"
}