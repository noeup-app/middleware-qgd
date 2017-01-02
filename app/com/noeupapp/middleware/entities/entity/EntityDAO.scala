package com.noeupapp.middleware.entities.entity

import java.sql.Connection
import java.util.UUID

import anorm._

import scala.language.postfixOps

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
}
