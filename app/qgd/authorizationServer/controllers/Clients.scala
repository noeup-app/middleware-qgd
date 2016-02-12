package qgd.authorizationServer
package controllers

import java.util.UUID
import javax.inject.Inject

import play.api.data.Forms._
import play.api.data._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, Controller}
import qgd.authorizationServer.forms.ClientForm
import qgd.authorizationServer.models.Client
import qgd.authorizationServer.utils.NamedLogger
import qgd.errorHandle.ExceptionEither._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import scalaz.{-\/, \/-}


class Clients @Inject()(implicit val messagesApi: MessagesApi) extends Controller with SecuredAdminConsole with NamedLogger {

  def list = withAuth { username => implicit request =>
    val allClients = models.Client.list()
    Ok(qgd.authorizationServer.views.html.clients.list(allClients, None))
  }

  def create() = withAuth { username => implicit request =>
    // generate unique and random values for id and secret
    val clientId = UUID.randomUUID().toString        // TODO : change format to fit RFC requierements
    val clientSecret = UUID.randomUUID().toString    // TODO : change format to fit RFC requierements
    val boundForm = ClientForm.form.bind(
      Map("id"     -> clientId,
          "secret" -> clientSecret)
    )
    Ok(qgd.authorizationServer.views.html.clients.new_client(boundForm, None))
  }

  def edit(id: String) = withAuth { username => implicit request =>
    val clientOpt = models.Client.findByClientId(id)
    clientOpt match {
      case None => NotFound
      case Some(client) =>
        val boundForm = ClientForm.form.bind(
          Map("id"          -> client.clientId,
              "name"        -> client.clientName,
              "secret"      -> client.clientSecret,
              "grantType"   -> client.authorizedGrantTypes.getOrElse(""),
              "description" -> client.description,
              "redirectUri" -> client.redirect_uri,
              "scope"       -> client.defaultScope.getOrElse(""))
        )
        Ok(qgd.authorizationServer.views.html.clients.edit_client(boundForm, None))
    }
  }

  def get(id: String) = withAuth { username => implicit request =>
    val clientOpt = models.Client.findByClientId(id)
    clientOpt match {
      case None => NotFound
      case Some(client) =>
        Ok(qgd.authorizationServer.views.html.clients.show_client(client, None))
    }
  }

  def delete(id: String) = Action {NotImplemented}

  def add = withAuth { username => implicit request =>
    request.method match {
      case "POST" =>
        val boundForm = ClientForm.form.bindFromRequest
        // validate rules
        boundForm.fold(

          formWithErrors => {
            logger.debug("Form has errors")
            logger.debug(formWithErrors.errors.toString)
            Ok(qgd.authorizationServer.views.html.clients.new_client(formWithErrors, None))
              .flashing("error" -> "Form has errors. Please enter correct values.")
          },

          clientDetails => {
            // check for duplicate
            models.Client.findByClientId(clientDetails.clientId) match {
              case None =>
                logger.debug("Saving new client")
                val client = Client(clientDetails.clientId,
                                    clientDetails.clientName,
                                    clientDetails.clientSecret,
                                    clientDetails.authorizedGrantTypes,
                                    clientDetails.description,
                                    clientDetails.redirect_uri,
                                    clientDetails.defaultScope)
                Try(models.Client.insert(client)) match {
                  case -\/(_) =>
                    logger.debug("Error while saving client entry")
                    Ok(qgd.authorizationServer.views.html.clients.new_client(boundForm, None))
                      .flashing("error" -> "Please try again")
                  case \/-(_) =>
                    logger.debug("Successfully added. Redirecting to client list")
                    Redirect(qgd.authorizationServer.controllers.routes.Clients.list)
                }
              case Some(c) => // duplicate
                logger.debug("Duplicate client entry")
                Ok(qgd.authorizationServer.views.html.clients.new_client(boundForm, None))
                  .flashing("error" -> "Please select another id for this client")
            }
          }
        )

      case "PUT" => NotImplemented
      case _ => BadRequest("wrong verb usage")
    }
  }

  def update =  withAuth { username => implicit request =>
    logger.debug("Clients.update()")
    val boundForm = ClientForm.form.bindFromRequest
    boundForm.fold(

      formWithErrors => {
        logger.debug("Form has errors")
        logger.debug(formWithErrors.errors.toString)
        Ok(qgd.authorizationServer.views.html.clients.new_client(formWithErrors, None))
          .flashing("error" -> "Form has errors. Please enter correct values.")
      },

      clientDetails => {
        logger.debug("Client details")
        logger.debug("Searching for client: " + clientDetails.clientId)
        models.Client.findByClientId(clientDetails.clientId) match {
          //models.oauth2.Clients.get(cd.id) match {
          case Some(c) => // existing client
            logger.debug("Saving new client")
            Try(models.Client.update(clientDetails)) match {
              case -\/(_) =>
                logger.debug("Error while saving client entry")
                Ok(qgd.authorizationServer.views.html.clients.new_client(boundForm, None)).flashing("error" -> "Please try again")
              case \/-(_) =>
                logger.debug("Successfully added. Redirecting to client list")
                Redirect(qgd.authorizationServer.controllers.routes.Clients.list)
            }
            logger.debug("redirecting to client list")
            Redirect(qgd.authorizationServer.controllers.routes.Clients.list)
          case None => // does not exist
            logger.debug("No such client")
            NotFound
        }
      }
    )
  }

}
