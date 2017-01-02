package com.noeupapp.middleware.entities.role

import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.entities.relationUserRole.RelationUserRoleDAO
import com.noeupapp.middleware.errorHandle.FailError.Expect

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.\/-

class RoleService  @Inject() (relationUserRole: RelationUserRoleDAO,
                              roleDAO: RoleDAO) {

  def addUserRoles(user: Account): Future[Boolean] = Future {
//    DB.withTransaction({ implicit c =>
//      // TODO
////      user.roles.map(relationUserRole.addRoleToUser(user.id, _))
//    })
    true
  }



  def getRoleByUser(userId: UUID): Future[Expect[List[String]]] =
    TryBDCall{ implicit c =>
      \/-(roleDAO.getByUserId(userId))
    }
}
