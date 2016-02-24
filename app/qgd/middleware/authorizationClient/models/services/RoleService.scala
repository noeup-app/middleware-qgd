package qgd.middleware.authorizationClient.models
package services


import play.api.Play.current
import javax.inject.Inject
import play.api.db.DB
import qgd.middleware.authorizationClient.models.daos.RelationUserRoleDAO
import qgd.middleware.models.Account

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RoleService  @Inject() (relationUserRole: RelationUserRoleDAO) {

  def addUserRoles(user: Account) = Future {
    DB.withTransaction({ implicit c =>
      user.roles.map(relationUserRole.addRoleToUser(user.id, _))
    })
  }
}
