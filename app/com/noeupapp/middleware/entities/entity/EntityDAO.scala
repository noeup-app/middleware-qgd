package com.noeupapp.middleware.entities.entity

import java.sql.Connection
import java.util.UUID

import anorm._
import com.noeupapp.middleware.entities.entity.Entity._

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

}
