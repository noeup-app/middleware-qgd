package qgd.middleware.authorizationClient.models
package services


import play.api.Play.current
import javax.inject.Inject
import play.api.db.DB
import qgd.middleware.authorizationClient.models.daos.RelationUserRoleDAO
import qgd.middleware.models.Account

import scala.concurrent.Future

class RoleService  @Inject() (relationUserRole: RelationUserRoleDAO) {

  def addUserRoles(user: Account) = DB.withTransaction({ implicit c => Future.successful {
    user.roles.map(relationUserRole.addRoleToUser(user.id, _))
  }
  })
}
