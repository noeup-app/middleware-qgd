package com.noeupapp.middleware.entities.role

import javax.inject.Inject

import com.noeupapp.middleware.entities.relationUserRole.RelationUserRoleDAO
import com.noeupapp.middleware.entities.user.{Account, User}
import play.api.Play.current
import play.api.db.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RoleService  @Inject() (relationUserRole: RelationUserRoleDAO) {

  def addUserRoles(user: Account): Future[Boolean] = Future {
    DB.withTransaction({ implicit c =>
      // TODO
//      user.roles.map(relationUserRole.addRoleToUser(user.id, _))
    })
    true
  }
}
