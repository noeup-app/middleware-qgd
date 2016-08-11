package com.noeupapp.middleware.crudauto

import java.sql.Connection
import java.util.UUID

import anorm.{~, _}
import com.noeupapp.middleware.entities.entity.Entity
import com.noeupapp.middleware.entities.entity.Entity._
import com.noeupapp.middleware.utils.GlobalReadsWrites
import play.api.Logger

import scala.language.postfixOps

class CrudAutoDAO extends GlobalReadsWrites {

  def findById[T](entityId: UUID, tableName: String, parser: RowParser[T])(implicit connection: Connection) = {

    SQL(
      s"""SELECT *
         FROM ${tableName}
         WHERE id = {id}::UUID;"""
      ).on(
        'id -> entityId
      ).as(parser *).headOption
  }

  def findAll[T](tableName: String, parser: RowParser[T])(implicit connection: Connection) = {

    SQL(
      s"""SELECT *
         FROM ${tableName};
        """
    ).as(parser *)
  }

  def add(tableName: String, param: String, value: String)(implicit connection: Connection): Boolean = {
    SQL(
      s"""
         INSERT INTO ${tableName} (${param})
             VALUES (${value});
      """
    ).execute()
  }
}