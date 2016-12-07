package com.noeupapp.middleware.utils.parser

import scalaz.syntax.traverse._
import scalaz.std.list._
import scalaz.std.option._

/**
  * Created by damien on 06/12/2016.
  */
object CSVSpitter {

  def splitCSV(lines: List[Line], cSVAction: CSVAction): List[Line] =
    lines.map(splitLine(_, cSVAction))

  def splitLine(line: Line, cSVAction: CSVAction): Line = {
    val cols = line.value.split(",").toList

    cSVAction match {
      case CSVKeepCol(from, to) =>
        line.copy(value = cols.slice(from, to).mkString(","))

      case CSVRemoveCol(from, to) =>
        line
          .copy(value =
            (cols.slice(0, from) ++ cols.slice(to, cols.length))
              .mkString(","))
    }
  }

}

sealed trait CSVAction

case class CSVRemoveCol(from: Int, to: Int) extends CSVAction
case class CSVKeepCol(from: Int, to: Int) extends CSVAction