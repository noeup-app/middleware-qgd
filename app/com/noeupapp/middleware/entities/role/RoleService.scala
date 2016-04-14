package com.noeupapp.middleware.entities.role

import javax.inject.Inject

import com.noeupapp.middleware.entities.entity.Account
import com.noeupapp.middleware.entities.relationUserRole.RelationUserRoleDAO
import play.api.Play.current
import play.api.db.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RoleService  @Inject() (relationUserRole: RelationUserRoleDAO) {

  def addUserRoles(user: Account) = Future {
    DB.withTransaction({ implicit c =>
      user.roles.map(relationUserRole.addRoleToUser(user.id, _))
    })
  }
}
