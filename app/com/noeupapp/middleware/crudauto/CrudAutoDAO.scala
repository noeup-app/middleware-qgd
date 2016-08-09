package com.noeupapp.middleware.crudauto

import java.sql.Connection
import java.util.UUID

import anorm.SqlParser._
import anorm.{~, _}
import com.noeupapp.middleware.entities.entity.Entity
import com.noeupapp.middleware.entities.entity.Entity._
import com.noeupapp.middleware.utils.GlobalReadsWrites
import play.api.Logger

import scala.language.postfixOps

class CrudAutoDAO extends GlobalReadsWrites {

  def findById[T](model: T, entityId: UUID, tableName: String)(implicit connection: Connection) = {
    val classe = model.asInstanceOf[Class[T]]
    val const = classe.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val parse = classe.getDeclaredField("parse")
    parse.setAccessible(true)
    val parser =  parse.get(classe.cast(obj)).asInstanceOf[anorm.RowParser[T]]

    SQL(
      s"""SELECT *
         FROM ${tableName}
         WHERE id = {id}::UUID;"""
      ).on(
        'id -> entityId
      ).as(parser *).headOption
  }
}