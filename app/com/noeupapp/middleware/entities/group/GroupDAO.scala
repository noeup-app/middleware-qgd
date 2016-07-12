package com.noeupapp.middleware.entities.group


import java.sql.Connection
import java.util.UUID

import anorm._
import com.noeupapp.middleware.utils.GlobalReadsWrites

import scala.language.postfixOps

class GroupDAO extends GlobalReadsWrites {

  def getById(groupId: UUID, userId: UUID)(implicit connection: Connection): Option[Group] = {
    SQL(
      """
          SELECT grou.id, grou.name, owner, grou.deleted
          FROM entity_groups grou
          INNER JOIN entity_entities ent ON ent.id = grou.id
          INNER JOIN entity_hierarchy hi ON hi.parent = ent.id
          WHERE grou.id = {id}::UUID
          AND (owner = {user}:UUID OR hi.entity = {user}:UUID OR {user}::UUID IN (
            SELECT ent.id
                 FROM entity_entities ent
                 INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
                 INNER JOIN entity_groups grou ON hi.parent = grou.id OR hi.entity = grou.owner
                 WHERE grou.name = 'Admin'
            ))
          AND grou.deleted = false
      """
    ).on(
      'id -> groupId,
      'user -> userId
    ).as(Group.parse *).headOption
  }

  def getAll(userId: UUID)(implicit connection: Connection): List[Group] = {
    SQL(
      """
          SELECT grou.id, grou.name, owner, grou.deleted
          FROM entity_groups grou
          INNER JOIN entity_entities ent ON ent.id = grou.id
          INNER JOIN entity_hierarchy hi ON hi.parent = ent.id
          WHERE owner = {user}:UUID OR hi.entity = {user}:UUID OR {user}::UUID IN (
                    SELECT ent.id
                         FROM entity_entities ent
                         INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
                         INNER JOIN entity_groups grou ON hi.parent = grou.id OR hi.entity = grou.owner
                         WHERE grou.name = 'Admin'
                    )
          AND grou.deleted = false
      """
    ).on(
      'user -> userId
    ).as(Group.parse *)
  }

  def isAdmin(userId: UUID)(implicit connection: Connection): Boolean = {
    SQL(
      """
         SELECT ent.id
                 FROM entity_entities ent
                 INNER JOIN entity_hierarchy hi ON hi.entity = ent.id
                 INNER JOIN entity_groups grou ON hi.parent = grou.id OR hi.entity = grou.owner
                 WHERE grou.name = 'Admin'
                 AND ent.id = {user}:UUID
      """
    ).on(
      'user -> userId
    ).execute()
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
