package com.noeupapp.middleware.entities.group


import java.sql.Connection
import java.util.UUID

import com.noeupapp.middleware.entities.entity.Entity

import anorm._
import com.noeupapp.middleware.utils.GlobalReadsWrites

import scala.language.postfixOps

class GroupDAO extends GlobalReadsWrites {

  def getById(groupId: UUID, userId: UUID, admin: Boolean)(implicit connection: Connection): Option[Group] = {
    SQL(
      """
          SELECT grou.id, grou.name, owner, grou.deleted
          FROM entity_groups grou
          INNER JOIN entity_entities ent ON ent.id = grou.id
          INNER JOIN entity_hierarchy hi ON hi.parent = ent.id
          WHERE grou.id = {id}::UUID
          AND (owner = {user}:UUID OR hi.entity = {user}:UUID OR {admin} = 'true'
          AND grou.deleted = false
      """
    ).on(
      'id -> groupId,
      'user -> userId,
      'admin -> admin
    ).as(Group.parse *).headOption
  }

  def getAll(userId: UUID, admin: Boolean)(implicit connection: Connection): List[Group] = {
    SQL(
      """
          SELECT grou.id, grou.name, owner, grou.deleted
          FROM entity_groups grou
          INNER JOIN entity_entities ent ON ent.id = grou.id
          INNER JOIN entity_hierarchy hi ON hi.parent = ent.id
          WHERE owner = {user}:UUID OR hi.entity = {user}:UUID OR {admin} = 'true'
          AND grou.deleted = false
      """
    ).on(
      'user -> userId,
      'admin -> admin
    ).as(Group.parse *)
  }

  def findAdmin(implicit connection: Connection): List[Entity] = {
    SQL(
      """
         SELECT *
                 FROM entity_entities ent
                 INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
                 INNER JOIN entity_groups grou ON hi.parent = grou.id OR hi.entity = grou.owner
                 WHERE grou.name = 'Admin'
      """
    ).as(Entity.parse *)
  }

  def findMembers(groupId: UUID)(implicit connection: Connection): List[GroupMember] = {
    SQL(
      """
          SELECT ent.id, first_name, last_name, org.name AS organisation_name, gro.name AS group_name
          FROM entity_entities ent
          INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
          INNER JOIN entity_groups grou ON grou.id = hi.parent
          LEFT JOIN entity_users user ON ent.id = user.id
          LEFT JOIN entity_organisations org ON org.id = ent.id
          LEFT JOIN entity_groups gro ON gro.id = ent.id
          WHERE grou.id = {id}::UUID
          AND grou.deleted = false
      """
    ).on(
      'id -> groupId
    ).as(Group.parseMember *)
  }

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
