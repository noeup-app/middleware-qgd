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
    Future.successful{
      Try {
        DB.withConnection({ implicit c =>
          userDAO.find(email)
        })
      }.toOption.flatten
    }
  }

  def findById(id: UUID): Future[Option[User]] = {
    Future.successful{
      Try {
        DB.withConnection({ implicit c =>
          userDAO.find(id)
        })
      }.toOption.flatten
    }
  }

  def add(user: User): Future[Boolean] = {
    Future.successful{
      Try {
        DB.withConnection({ implicit c =>
          userDAO.add(user)
        })
      }.toOption.getOrElse(false)
    }
  }

}
