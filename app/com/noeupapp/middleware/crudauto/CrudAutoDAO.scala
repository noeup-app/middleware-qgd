package com.noeupapp.middleware.crudauto

import java.lang.reflect.Field
import java.sql.Connection
import java.util.UUID

import anorm._
import com.noeupapp.middleware.utils.GlobalReadsWrites
import play.api.Logger

import scala.language.postfixOps

class CrudAutoDAO extends GlobalReadsWrites {

  def findById[T](entityId: UUID, tableName: String, parser: RowParser[T])(implicit connection: Connection) = {

    SQL(
      s"""SELECT *
         FROM $tableName
         WHERE id = {id}::UUID;"""
      ).on(
        'id -> entityId
      ).as(parser *).headOption
  }

  def findAll[T](tableName: String, parser: RowParser[T])(implicit connection: Connection) = {

    SQL(
      s"""SELECT *
         FROM $tableName;
        """
    ).as(parser *)
  }

  def add[T, A](tableName: String, entity:T, singleton: Class[A], params: String, values: String)(implicit connection: Connection): Boolean = {

    SQL(
      s"""
         INSERT INTO $tableName ($params)
             VALUES ($values);
      """
    ).execute()
  }

  def update(tableName: String, value: String, id: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      s"""
        UPDATE $tableName
        SET
          $value
        WHERE id = {id}::UUID
      """
    ).on(
      'id -> id
    ).execute()
  }

  def delete(tableName: String, id: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      s"""
        UPDATE $tableName
        SET deleted = 'true'
        WHERE id = {id}::UUID
      """
    ).on(
      'id -> id
    ).execute()
  }
}