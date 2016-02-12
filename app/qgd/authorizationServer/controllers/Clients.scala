package qgd.authorizationServer
package controllers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import qgd.authorizationServer.models.Client
import qgd.errorHandle.ExceptionEither._
import play.api.Play.current
import qgd.resourceServer.models.Account

import scalaz.{-\/, \/-}


class Clients @Inject()(
                         val messagesApi: MessagesApi,
                         val env: Environment[Account, CookieAuthenticator])
  extends Silhouette[Account, CookieAuthenticator]{

  val log = play.Logger.of("application")

  val clientForm = Form(mapping(
    "id" -> nonEmptyText,
    "name" -> nonEmptyText,
    "secret" -> nonEmptyText,
    "grantType" -> optional(text),
    "description" -> nonEmptyText,
    "redirectUri" -> nonEmptyText,
    "scope" -> optional(text))(Client.apply)(Client.unapply))

  def list = SecuredAction { implicit request =>
    val allClients = models.Client.list()
    Ok(qgd.authorizationServer.views.html.clients.list(allClients, None))
  }

  def create() = SecuredAction { implicit request =>
      // generate unique and random values for id and secret
      val clientId = UUID.randomUUID().toString        // TODO : change format to fit RFC requierements
      val clientSecret = UUID.randomUUID().toString    // TODO : change format to fit RFC requierements
      val boundForm = clientForm.bind(
        Map("id" -> clientId,
            "secret" -> clientSecret)
      )
      Ok(qgd.authorizationServer.views.html.clients.new_client(boundForm, None))
  }

  def edit(id: String) = SecuredAction { implicit request =>
    val clientOpt = models.Client.findByClientId(id)
    clientOpt match {
      case None => NotFound
      case Some(client) =>
        val boundForm = clientForm.bind(
          Map("id" -> client.clientId,
              "name" -> client.clientName,
              "secret" -> client.clientSecret,
              "grantType" -> client.authorizedGrantTypes.getOrElse(""),
              "description" -> client.description,
              "redirectUri" -> client.redirect_uri,
              "scope" -> client.defaultScope.getOrElse(""))
          )
        Ok(qgd.authorizationServer.views.html.clients.edit_client(boundForm, None))
    }
  }

  def get(id: String) = SecuredAction { implicit request =>
    val clientOpt = models.Client.findByClientId(id)
    clientOpt match {
      case None => NotFound
      case Some(client) =>
        Ok(qgd.authorizationServer.views.html.clients.show_client(client, None))
    }
  }

  def delete(id: String) = Action {NotImplemented}

  def add = SecuredAction { implicit request =>
     request.method match {
        case "POST" =>
          val boundForm = clientForm.bindFromRequest
          // validate rules
          boundForm.fold(

            formWithErrors => {
              log.debug("Form has errors")
              log.debug(formWithErrors.errors.toString)
              Ok(qgd.authorizationServer.views.html.clients.new_client(formWithErrors, None))
                .flashing("error" -> "Form has errors. Please enter correct values.")
            },

            clientDetails => {
              // check for duplicate
                models.Client.findByClientId(clientDetails.clientId) match {
                  case None =>
                    log.debug("Saving new client")
                    val client = Client(clientDetails.clientId,
                                        clientDetails.clientName,
                                        clientDetails.clientSecret,
                                        clientDetails.authorizedGrantTypes,
                                        clientDetails.description,
                                        clientDetails.redirect_uri,
                                        clientDetails.defaultScope)
                    Try(models.Client.insert(client)) match {
                      case -\/(_) =>
                        log.debug("Error while saving client entry")
                        Ok(qgd.authorizationServer.views.html.clients.new_client(boundForm, None)).flashing("error" -> "Please try again")
                      case \/-(_) =>
                        log.debug("Successfully added. Redirecting to client list")
                        Redirect(qgd.authorizationServer.controllers.routes.Clients.list)
                    }
                  case Some(c) => // duplicate
                    log.debug("Duplicate client entry")
                    Ok(qgd.authorizationServer.views.html.clients.new_client(boundForm, None)).flashing("error" -> "Please select another id for this client")
                }
              }
            )

        case "PUT" => NotImplemented
        case _ => BadRequest("wrong verb usage")
      }
  }

  def update =  SecuredAction { implicit request =>
      log.debug("Clients.update()")
      val boundForm = clientForm.bindFromRequest
      boundForm.fold(

        formWithErrors => {
          log.debug("Form has errors")
          log.debug(formWithErrors.errors.toString)
          Ok(qgd.authorizationServer.views.html.clients.new_client(formWithErrors, None))
            .flashing("error" -> "Form has errors. Please enter correct values.")
        },

        clientDetails => {
          log.debug("Client details")
          log.debug("Searching for client: " + clientDetails.clientId)
          models.Client.findByClientId(clientDetails.clientId) match {
          //models.oauth2.Clients.get(cd.id) match {
              case Some(c) => // existing client
                log.debug("Saving new client")
                Try(models.Client.update(clientDetails)) match {
                  case -\/(_) =>
                    log.debug("Error while saving client entry")
                    Ok(qgd.authorizationServer.views.html.clients.new_client(boundForm, None)).flashing("error" -> "Please try again")
                  case \/-(_) =>
                    log.debug("Successfully added. Redirecting to client list")
                    Redirect(qgd.authorizationServer.controllers.routes.Clients.list)
                }
                log.debug("redirecting to client list")
                Redirect(qgd.authorizationServer.controllers.routes.Clients.list)
              case None => // does not exist
                log.debug("No such client")
                NotFound
            }
          }
        )
  }

}
