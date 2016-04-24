package com.noeupapp.middleware.entities.organisation

import java.util.UUID
import java.sql.Connection

import anorm._

import scala.language.postfixOps

class OrganisationDAO {

  def findById(organisationId: UUID)(implicit connection: Connection): Option[Organisation] = {
    SQL(
      """SELECT *
         FROM entity_organisations
         WHERE id = {id}::UUID;""")
      .on(
        'id -> organisationId
      ).as(Organisation.parseDB *).headOption
  }



}
