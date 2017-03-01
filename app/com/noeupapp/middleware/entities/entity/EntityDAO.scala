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

  /**
    * Do an union get packageId from a userId
    * If user is in an orga, get packageId orga
    *
    * @param userId
    * @param connection
    * @return user packageId or organisation packageId
    */
  def getPackageIdFromUser(userId: UUID)(implicit connection: Connection): Option[Long] = {
    SQL(
      """
       (SELECT package_id from entity_entities
          WHERE id = {userId}::UUID)
       UNION
       (SELECT e_orga.package_id from entity_entities e_user
          INNER JOIN entity_entities e_orga ON e_user.parent = e_orga.id
          WHERE e_user.id = {userId}::UUID)"""
    ).on('userId -> userId)
      .as(SqlParser.scalar[Long].singleOpt)
  }
}
