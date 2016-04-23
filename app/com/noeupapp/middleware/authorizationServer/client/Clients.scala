package com.noeupapp.middleware.authorizationServer.client

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.RoleAuthorization.WithRole
import com.noeupapp.middleware.authorizationClient.ScopeAuthorization.WithScope
import com.noeupapp.middleware.authorizationClient.{RoleAuthorization, ScopeAndRoleAuthorization, ScopeAuthorization}
import com.noeupapp.middleware.authorizationServer.client
import com.noeupapp.middleware.entities.account.Account
import com.noeupapp.middleware.entities.user.User
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import com.noeupapp.middleware.errorHandle.ExceptionEither._

import scalaz.{-\/, \/-}

// TODO implement switch json/html

class Clients @Inject()(
                         val messagesApi: MessagesApi,
                         val env: Environment[Account, BearerTokenAuthenticator],
                         scopeAndRoleAuthorization: ScopeAndRoleAuthorization
                       ) extends Silhouette[Account, BearerTokenAuthenticator] {

  def list = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))) { implicit request =>
    val allClients = client.Client.list()
    Ok(com.noeupapp.middleware.authorizationServer.client.html.list(allClients, None))
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

  def edit(id: String) = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))) { implicit request =>
    val clientOpt = client.Client.findByClientId(id)
    clientOpt match {
      case None => NotFound
      case Some(client) =>
        val boundForm = ClientForm.form.bind( // TODO extract form
          Map("id"          -> client.clientId,
              "name"        -> client.clientName,
              "secret"      -> client.clientSecret,
              "grantType"   -> client.authorizedGrantTypes.getOrElse(""),
              "description" -> client.description,
              "redirectUri" -> client.redirect_uri,
              "scope"       -> client.defaultScope.getOrElse(""))
        )
        Ok(com.noeupapp.middleware.authorizationServer.client.html.edit_client(boundForm, None))
    }
  }

  def get(id: String) = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))) { implicit request =>
    val clientOpt = client.Client.findByClientId(id)
    clientOpt match {
      case None => NotFound
      case Some(client) =>
        Ok(com.noeupapp.middleware.authorizationServer.client.html.show_client(client, None))
    }
  }

  def delete(id: String) = Action {NotImplemented}


  def add = SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))) { implicit request =>
    request.method match {
      case "POST" =>
        val boundForm = ClientForm.form.bindFromRequest
        boundForm.fold(

          formWithErrors => {
            logger.debug("Form has errors")
            logger.debug(formWithErrors.errors.toString)
            Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(formWithErrors, None))
              .flashing("error" -> "Form has errors. Please enter correct values.")
          },

          client => {
            // check for duplicate
            Client.findByClientId(client.clientId) match {
              case None =>
                logger.debug("Saving new client")
                Try(Client.insert(client)) match {
                  case -\/(_) =>
                    logger.debug("Error while saving client entry")
                    Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(boundForm, None))
                      .flashing("error" -> "Please try again")
                  case \/-(_) =>
                    logger.debug("Successfully added. Redirecting to client list")
                    Redirect(com.noeupapp.middleware.authorizationServer.client.routes.Clients.list())
                }
              case Some(c) => // duplicate
                logger.debug("Duplicate client entry")
                Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(boundForm, None))
                  .flashing("error" -> "Please select another id for this client")
            }
          }
        )

      case "PUT" => NotImplemented
      case _ => BadRequest("wrong verb usage")
    }
  }

  def update =  SecuredAction(scopeAndRoleAuthorization(WithScope(), WithRole("admin"))) { implicit request =>
    logger.debug("Clients.update()")
    val boundForm = ClientForm.form.bindFromRequest
    boundForm.fold(

      formWithErrors => {
        logger.debug("Form has errors")
        logger.debug(formWithErrors.errors.toString)
        Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(formWithErrors, None))
          .flashing("error" -> "Form has errors. Please enter correct values.")
      },

      clientDetails => {
        logger.debug("Client details")
        logger.debug("Searching for client: " + clientDetails.clientId)
        Client.findByClientId(clientDetails.clientId) match {
          //models.oauth2.Clients.get(cd.id) match {
          case Some(c) => // existing client
            logger.debug("Saving new client")
            Try(Client.update(clientDetails)) match {
              case -\/(_) =>
                logger.debug("Error while saving client entry")
                Ok(com.noeupapp.middleware.authorizationServer.client.html.new_client(boundForm, None)).flashing("error" -> "Please try again")
              case \/-(_) =>
                logger.debug("Successfully added. Redirecting to client list")
                Redirect(com.noeupapp.middleware.authorizationServer.client.routes.Clients.list())
            }
            logger.debug("redirecting to client list")
            Redirect(com.noeupapp.middleware.authorizationServer.client.routes.Clients.list())
          case None => // does not exist
            logger.debug("No such client")
            NotFound
        }
      }
    )
  }

}
