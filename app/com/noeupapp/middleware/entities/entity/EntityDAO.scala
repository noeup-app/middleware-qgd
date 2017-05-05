package com.noeupapp.middleware.entities.entity

import java.sql.Connection
import java.util.UUID

import anorm._

import scala.language.postfixOps
import com.noeupapp.middleware.entities.group.Group
class EntityDAO {

  def findById(entityId: UUID)(implicit connection: Connection) = {
    SQL(
      """SELECT *
         FROM entity_entities
         WHERE id = {id}::UUID;""")
      .on(
        'id -> entityId
      ).as(Entity.parse *).headOption
  }

  /**
    * Add a new hierarchy between an entity and a parent entity
    *
    * @param parentId
    * @param entityId
    * @param connection
    * @return
    */
  def addHierarchy(parentId: UUID, entityId: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      """
          INSERT INTO entity_hierarchy (entity, parent)
          VALUES ({entity}::UUID,
                  {parent}::UUID)
      """
    ).on(
      'entity -> entityId,
      'parent -> parentId
    ).execute()
  }

  /**
    *
    * @param parentId
    * @param entityId
    * @param connection
    * @return
    */
  def removeHierarchy(entityId: UUID, parentId: Option[UUID])(implicit connection: Connection): Boolean = {
    parentId match {
      case None =>
        SQL(
          """
        DELETE FROM entity_hierarchy AS enth
        WHERE enth.entity = {entity}::UUID
          """
        ).on(
          'entity -> entityId
        ).execute()
      case Some(parent) =>
        SQL(
          """
        DELETE FROM entity_hierarchy AS enth
        WHERE enth.entity = {entity}::UUID AND enth.parent = {parent}::UUID
          """
        ).on(
          'entity -> entityId,
          'parent -> parent
        ).execute()

    }
  }

  def getHierarchy(entityId: UUID)(implicit connection: Connection) = {
    SQL(
      """SELECT entg.*
         FROM entity_hierarchy AS enth
         INNER JOIN entity_groups AS entg ON enth.entity = entg.id
         WHERE enth.entity = {id}::UUID;""")
      .on(
        'id -> entityId
      ).as(Group.parse *)
  }

}
