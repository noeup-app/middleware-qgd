package com.noeupapp.middleware.authorizationServer.client

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.noeupapp.middleware.authorizationClient.RoleAuthorization.WithRole
import com.noeupapp.middleware.authorizationClient.ScopeAuthorization.WithScope
import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.authorizationClient.customAuthenticator.CookieBearerTokenAuthenticator
import com.noeupapp.middleware.entities.account.Account
import play.api.i18n.MessagesApi
import play.api.Logger

import scala.concurrent.Future
import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

// TODO implement switch json/html

class Clients @Inject()(
                         val messagesApi: MessagesApi,
                         val env: Environment[Account, CookieBearerTokenAuthenticator],
                         scopeAndRoleAuthorization: ScopeAndRoleAuthorization,
                         clientService: ClientService
                       ) extends Silhouette[Account, CookieBearerTokenAuthenticator] {

  def list = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))).async { implicit request =>
    clientService.findAll() map {
      case \/-(clients) =>
        Ok(com.noeupapp.middleware.authorizationServer.client.html.list(clients, None))
      case -\/(e) =>
        Logger.error(e.toString)
        InternalServerError("Internal server error")
    }
  }

  def create() = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))) { implicit request =>
      // generate unique and random values for id and secret
      val clientId = UUID.randomUUID().toString        // TODO : change format to fit RFC requierements
      val clientSecret = UUID.randomUUID().toString    // TODO : change format to fit RFC requierements
      val boundForm = ClientForm.form.bind(
        Map("id" -> clientId,
            "secret" -> clientSecret)
      )
      Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(boundForm, None))
  }

  def edit(id: String) = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))).async { implicit request =>
    clientService.findByClientId(id) map {
      case -\/(e) =>
        Logger.error(e.toString)
        InternalServerError("Internal server error")
      case \/-(None) => NotFound("Client is not found")
      case \/-(Some(client)) =>
        val boundForm = ClientForm.form.bind(// TODO extract form
          Map("id" -> client.clientId,
              "name" -> client.clientName,
              "secret" -> client.clientSecret,
              "grantType" -> client.authorizedGrantTypes.getOrElse(""),
              "description" -> client.description,
              "redirectUri" -> client.redirect_uri,
              "scope" -> client.defaultScope.getOrElse(""))
        )
        Ok(com.noeupapp.middleware.authorizationServer.client.html.edit_client(boundForm, None))
    }
  }


  def get(id: String) = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))).async { implicit request =>
    clientService.findByClientId(id) map {
      case -\/(e) =>
        Logger.error(e.toString)
        InternalServerError("Internal server error")
      case \/-(None) => NotFound("Client is not found")
      case \/-(Some(client)) =>
        Ok(com.noeupapp.middleware.authorizationServer.client.html.show_client(client, None))
    }
  }


  def delete(id: String) = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))).async { implicit request =>
    clientService.delete(id) map {
      case -\/(e) =>
        Logger.error(e.toString)
        InternalServerError("Internal server error")
      case \/-(_) => Ok("Client deleted")
    }
  }


  def add = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))).async { implicit request =>
    val boundForm = ClientForm.form.bindFromRequest
    boundForm.fold(

      formWithErrors => Future.successful {
        Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(formWithErrors, None))
          .flashing("error" -> "Form has errors. Please enter correct values.")
      },

      client => {
        clientService.findByClientId(client.clientId) flatMap {
          case -\/(e) =>
            Logger.error(e.toString)
            Future.successful(InternalServerError("Internal server error"))
          case \/-(Some(_)) =>
            Future.successful(
              Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(boundForm, None))
              .flashing("error" -> "Please select another id for this client")
            )
          case \/-(None) =>
            clientService.insert(client) map {
              case -\/(_) =>
                Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(boundForm, None))
                  .flashing("error" -> "Please try again")
              case \/-(_) =>
                Redirect(com.noeupapp.middleware.authorizationServer.client.routes.Clients.list())
            }
        }
      }
    )
  }

  def update =  SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))).async { implicit request =>
    val boundForm = ClientForm.form.bindFromRequest
    boundForm.fold(

      formWithErrors => Future.successful {
        Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(formWithErrors, None))
          .flashing("error" -> "Form has errors. Please enter correct values.")
      },

      client => {
        clientService.findByClientId(client.clientId) flatMap {
          case -\/(e) =>
            Logger.error(e.toString)
            Future.successful(InternalServerError("Internal server error"))
          case \/-(Some(_)) =>
            Future.successful(
              Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(boundForm, None))
                .flashing("error" -> "Please select another id for this client")
            )
          case \/-(None) =>
            clientService.update(client) map {
              case -\/(_) =>
                Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(boundForm, None))
                  .flashing("error" -> "Please try again")
              case \/-(_) =>
                Redirect(com.noeupapp.middleware.authorizationServer.client.routes.Clients.list())
            }
        }
      }
    )
  }

}
