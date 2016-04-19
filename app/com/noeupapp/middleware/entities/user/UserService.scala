package com.noeupapp.middleware.entities.user

import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import com.noeupapp.middleware.entities.role.RoleService
import play.api.Logger
import play.api.Play.current
import play.api.db.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try


class UserService @Inject()(userDAO: UserDAO) {

  def findByEmail(email: String): Future[Option[User]] = {
    Future{
      try {
        DB.withConnection({ implicit c =>
          userDAO.find(email)
        })
      } catch {
        case e: Exception => Logger.error(s"UserService.findByEmail($email)", e)
          None
      }
    }
  }

  def findById(id: UUID): Future[Option[User]] = {
    Future{
      try {
        DB.withConnection({ implicit c =>
          userDAO.find(id)
        })
      } catch {
        case e: Exception =>
          Logger.error(s"UserService.findById($id)", e)
          None
      }
    }
  }

  def add(user: User): Future[Boolean] = {
    Future{
      try {
        DB.withConnection({ implicit c =>
          userDAO.add(user)
        })
      } catch {
        case e: Exception =>
          Logger.error(s"UserService.add($user)", e)
          false
      }
    }
  }

}
