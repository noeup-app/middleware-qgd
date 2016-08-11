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

  def add[T](entity: T, tableName: String)(implicit connection: Connection): Boolean = {
    true
  }
    /*SQL(
      """
         INSERT INTO courses (id, form, level, certification, module_name, requirements, proposals, locked, deleted)
             VALUES ({id}::UUID, {form}::UUID, {level}, {certification}::UUID, {module_name}, {requirements}::UUID, {proposals}, {locked}, {deleted});
      """
    ).on(
      'id -> course.id,
      'form -> course.formId,
      'level -> course.level,
      'certification -> course.certification,
      'module_name -> course.moduleName,
      'requirements -> course.requirements,
      'proposals -> course.proposals,
      'locked -> course.locked,
      'deleted -> course.deleted
    ).execute()
  }*/
}