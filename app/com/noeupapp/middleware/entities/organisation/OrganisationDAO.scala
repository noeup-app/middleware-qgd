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

  def add(organisation: Organisation)(implicit connection: Connection): Boolean = {
    SQL(
      """
          INSERT INTO entity_organisations (id, name, subdomain, logo_url, color, credits, deleted)
          VALUES ({id}::UUID,
                  {name},
                  {subdomain},
                  {logo_url},
                  {color},
                  {credits},
                  {deleted})
      """
    ).on(
      'id -> organisation.id,
      'name -> organisation.name,
      'subdomain -> organisation.sub_domain,
      'logo_url -> organisation.logo_url,
      'color -> organisation.color,
      'credits -> organisation.credits,
      'deleted -> organisation.deleted
    ).execute()
  }

}
