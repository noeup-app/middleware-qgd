package com.noeupapp.middleware.entities.group


import java.sql.Connection
import java.util.UUID

import com.noeupapp.middleware.entities.entity.{Entity, EntityOut}
import anorm._
import com.noeupapp.middleware.utils.GlobalReadsWrites

import scala.language.postfixOps

class GroupDAO extends GlobalReadsWrites {

  /**
    * Get a group by its ID if user is either an admin, a member or the group's owner
    *
    * @param groupId
    * @param userId
    * @param admin
    * @param connection
    * @return
    */
  def getById(groupId: UUID, userId: UUID, admin: Boolean, organisation: UUID)(implicit connection: Connection): Option[Group] = {
    SQL(
      """
          SELECT grou.id, grou.name, owner, grou.deleted
          FROM entity_groups grou
          INNER JOIN entity_entities ent ON ent.id = grou.id
          LEFT JOIN entity_hierarchy hi ON hi.parent = ent.id
          LEFT JOIN entity_hierarchy ho ON ho.entity = ent.id
          WHERE grou.id = {id}::UUID
          AND (owner = {user}::UUID OR hi.entity = {user}::UUID OR {admin} = 'true')
          AND ho.parent = {organisation}::UUID
          AND grou.deleted = false
      """
    ).on(
      'id -> groupId,
      'user -> userId,
      'admin -> admin,
      'organisation -> organisation
    ).as(Group.parse *).headOption
  }

  /**
    * Get all groups if user is admin
    * Otherwise, get only groups where user is a member or the owner
    *
    * @param userId
    * @param admin
    * @param connection
    * @return
    */
  def getAll(userId: UUID/*, admin: Boolean, organisation: UUID*/)(implicit connection: Connection): List[Group] = {
    SQL(
      """
          SELECT DISTINCT grou.id, grou.name, owner, grou.deleted
          FROM entity_groups grou
          INNER JOIN entity_entities ent ON ent.id = grou.id
          LEFT JOIN entity_hierarchy hi ON hi.parent = ent.id
          LEFT JOIN entity_hierarchy ho ON ho.entity = ent.id
          WHERE owner = {user}::UUID OR hi.entity = {user}::UUID
          --AND ho.parent = {organisation}::UUID
          AND grou.deleted = false
      """
    ).on(
      'user -> userId
      //'admin -> admin,
      //'organisation -> organisation
    ).as(Group.parse *)
  }

  /**
    * Find all entities member of "Admin" group
    *
    * @param connection
    * @return
    */
  def findAdmin(organisation: UUID)(implicit connection: Connection): List[Entity] = {
    SQL(
      """
         SELECT ent.id, ent.parent, ent.type, ent.account_type
         FROM entity_entities ent
         INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
         INNER JOIN entity_groups grou ON hi.parent = grou.id OR hi.entity = grou.owner
         LEFT JOIN entity_hierarchy ho ON ho.entity = ent.id
         WHERE grou.name = 'Admin'
         AND ho.parent = {organisation}::UUID
      """
    ).on(
      'organisation -> organisation
    ).as(Entity.parse *)
  }

  /**
    * Find all entities member of a group and return their name
    *
    * @param groupId
    * @param connection
    * @return
    */
  def findMembers(groupId: UUID, organisation: UUID)(implicit connection: Connection): List[EntityOut] = {
    SQL(
      """
          SELECT ent.id, first_name, last_name, org.name AS organisation_name
          FROM entity_entities ent
          INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
          INNER JOIN entity_groups grou ON grou.id = hi.parent
          LEFT JOIN entity_users use ON ent.id = use.id
          LEFT JOIN entity_organisations org ON org.id = ent.id
          LEFT JOIN entity_groups gro ON gro.id = ent.id
          LEFT JOIN entity_hierarchy ho ON ho.entity = ent.id
          WHERE grou.id = {id}::UUID
          AND ho.parent = {organisation}::UUID
          AND grou.deleted = false
      """
    ).on(
      'id -> groupId,
      'organisation -> organisation
    ).as(Entity.parseOut *)
  }

  /**
    * Add a new group
    *
    * @param group
    * @param connection
    * @return
    */
  def add(group: Group)(implicit connection: Connection): Boolean = {
    SQL(
      """
          INSERT INTO entity_groups (id, name, owner, deleted)
          VALUES ({id}::UUID,
                  {name},
                  {owner}::UUID,
                  {deleted})
      """
    ).on(
      'id -> group.id,
      'name -> group.name,
      'owner -> group.owner,
      'deleted -> group.deleted
    ).execute()
  }

  /**
    * Update a group
    *
    * @param group
    * @param connection
    * @return
    */
  def update(group: Group, organisation: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      """
         UPDATE entity_groups AS grou
         SET
            grou.name = {name},
            grou.owner = {owner}::UUID,
            grou.deleted = {deleted}
         FROM entity_entities ent
         INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
         WHERE grou.id = {id}::UUID
         AND ent.id = grou.id
         AND hi.parent = {organisation}::UUID
      """
    ).on(
      'id -> group.id,
      'name -> group.name,
      'owner -> group.owner,
      'organisation -> organisation,
      'deleted -> group.deleted
    ).execute()
  }

  /**
    * Delete a group
    *
    * @param groupId
    * @param connection
    * @return
    */
  def delete(groupId: UUID, organisation: UUID, force_delete: Boolean)(implicit connection: Connection): Boolean = {
    force_delete match {
      case false =>
        SQL(
          """
               UPDATE entity_groups AS grou
               SET deleted = 'true'
               FROM entity_entities ent
                        INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
                        WHERE grou.id = {id}::UUID
                        AND ent.id = grou.id
                        AND hi.parent = {organisation}::UUID
            """
          ).on(
            'id -> groupId,
            'organisation -> organisation
          ).execute()
      case true =>
        SQL(
          """
               DELETE FROM entity_groups AS grou
               WHERE grou.id = {id}::UUID
          """
        ).on(
          'id -> groupId
        ).execute()
        }
  }
}
