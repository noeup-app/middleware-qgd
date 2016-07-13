package com.noeupapp.middleware.entities.group


import java.sql.Connection
import java.util.UUID

import com.noeupapp.middleware.entities.entity.Entity

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
  def getById(groupId: UUID, userId: UUID, admin: Boolean)(implicit connection: Connection): Option[Group] = {
    SQL(
      """
          SELECT grou.id, grou.name, owner, grou.deleted
          FROM entity_groups grou
          INNER JOIN entity_entities ent ON ent.id = grou.id
          LEFT JOIN entity_hierarchy hi ON hi.parent = ent.id
          WHERE grou.id = {id}::UUID
          AND (owner = {user}::UUID OR hi.entity = {user}::UUID OR {admin} = 'true')
          AND grou.deleted = false
      """
    ).on(
      'id -> groupId,
      'user -> userId,
      'admin -> admin
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
  def getAll(userId: UUID, admin: Boolean)(implicit connection: Connection): List[Group] = {
    SQL(
      """
          SELECT DISTINCT grou.id, grou.name, owner, grou.deleted
          FROM entity_groups grou
          INNER JOIN entity_entities ent ON ent.id = grou.id
          LEFT JOIN entity_hierarchy hi ON hi.parent = ent.id
          WHERE owner = {user}::UUID OR hi.entity = {user}::UUID OR {admin} = 'true'
          AND grou.deleted = false
      """
    ).on(
      'user -> userId,
      'admin -> admin
    ).as(Group.parse *)
  }

  /**
    * Find all entities member of "Admin" group
    *
    * @param connection
    * @return
    */
  def findAdmin(implicit connection: Connection): List[Entity] = {
    SQL(
      """
         SELECT ent.id, ent.parent, ent.type, ent.account_type
         FROM entity_entities ent
         INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
         INNER JOIN entity_groups grou ON hi.parent = grou.id OR hi.entity = grou.owner
         WHERE grou.name = 'Admin'
      """
    ).as(Entity.parse *)
  }

  /**
    * Find all entities member of a group and return their name
    *
    * @param groupId
    * @param connection
    * @return
    */
  def findMembers(groupId: UUID)(implicit connection: Connection): List[GroupMember] = {
    SQL(
      """
          SELECT ent.id, first_name, last_name, org.name AS organisation_name, gro.name AS group_name
          FROM entity_entities ent
          INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
          INNER JOIN entity_groups grou ON grou.id = hi.parent
          LEFT JOIN entity_users use ON ent.id = use.id
          LEFT JOIN entity_organisations org ON org.id = ent.id
          LEFT JOIN entity_groups gro ON gro.id = ent.id
          WHERE grou.id = {id}::UUID
          AND grou.deleted = false
      """
    ).on(
      'id -> groupId
    ).as(Group.parseMember *)
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
    * Add a new hierarchy between an entity and a parent group
    *
    * @param groupId
    * @param entityId
    * @param connection
    * @return
    */
  def addHierarchy(groupId: UUID, entityId: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      """
          INSERT INTO entity_hierarchy (entity, parent)
          VALUES ({entity}::UUID,
                  {parent}::UUID)
      """
    ).on(
      'entity -> entityId,
      'parent -> groupId
    ).execute()
  }

  /**
    * Update a group
    *
    * @param group
    * @param connection
    * @return
    */
  def update(group: Group)(implicit connection: Connection): Boolean = {
    SQL(
      """
         UPDATE entity_groups
         SET
            name = {name},
            owner = {owner}::UUID,
            deleted = {deleted}
         WHERE id = {id}::UUID
      """
    ).on(
      'id -> group.id,
      'name -> group.name,
      'owner -> group.owner,
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
  def delete(groupId: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      """
    UPDATE entity_groups
    SET deleted = 'true'
    WHERE id = {id}::UUID
      """
    ).on(
      'id -> groupId
    ).execute()
  }
}
