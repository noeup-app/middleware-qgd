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

  def add[T, A](tableName: String, entity:T, singleton: A, params: String, values: String)(implicit connection: Connection): Boolean = {
    /*val sing = singleton.asInstanceOf[Class[A]]
    val const = sing.getDeclaredConstructors()(0)
    const.setAccessible(true)
    val obj = const.newInstance()
    val getTableColumnNames = sing.getDeclaredMethod("getTableColumns", classOf[String])
    getTableColumnNames.setAccessible(true)
    val fields = entity.getClass.getDeclaredFields
    val params = fields.flatMap{field => getTableColumnNames.invoke(obj, field.getName).asInstanceOf[Option[String]] }
    val values = fields.map{field => field.setAccessible(true)
      val classe = field.getType
      getValue(entity, classe, field)}
    Logger.debug(values.toSeq.toString)
    Logger.debug(values.map{v => v.getClass.getName}.toSeq.toString())*/

    SQL(
      s"""
         INSERT INTO $tableName ($params)
             VALUES ({$values});
      """
    ).execute()
  }

  def getValue[T, A](entity: A, fieldType: Class[T], field: Field):T = {
    val fi = field.get(entity).asInstanceOf[T]
    Logger.debug(fi.getClass.getName)
    Logger.debug(fi.toString)
    fi
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