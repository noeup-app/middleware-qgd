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

  def findById[T](classe: T, entityId: UUID, tableName: String)(implicit connection: Connection) = {
    Logger.debug(classe.asInstanceOf[Class[T]].getName)
    val parser = classe.asInstanceOf[Class[T]].getDeclaredField("parse")
    parser.setAccessible(true)
    Logger.debug(parser.toString)
    SQL(
      s"""SELECT *
         FROM ${tableName}
         WHERE id = {id}::UUID;"""
      ).on(
        'id -> entityId
      ).as(Entity.parse.asInstanceOf[anorm.RowParser[T]] *).headOption
  }
}