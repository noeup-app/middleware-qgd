package com.noeupapp.middleware.entities.organisation

import java.util.UUID
import javax.inject.Inject

import com.noeupapp.middleware.entities.organisation.Organisation._
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.BearerTokenAuthenticator
import com.noeupapp.middleware.authorizationClient.RoleAuthorization.WithRole
import com.noeupapp.middleware.authorizationClient.ScopeAndRoleAuthorization
import com.noeupapp.middleware.authorizationClient.ScopeAuthorization.WithScope
import com.noeupapp.middleware.entities.account.Account
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.json.Json

import scalaz._
import scala.concurrent.ExecutionContext.Implicits.global



class Organisations @Inject()(
                               val messagesApi: MessagesApi,
                               val env: Environment[Account, BearerTokenAuthenticator],
                               scopeAndRoleAuthorization: ScopeAndRoleAuthorization,
                               organisationService: OrganisationService)
  extends Silhouette[Account, BearerTokenAuthenticator] {

  def addOrganisation() = SecuredAction(scopeAndRoleAuthorization(WithScope(/*builder.organisations*/), WithRole("admin")))
    .async(parse.json[OrganisationIn]) { implicit request =>
      val organisationIn = request.request.body
      val user = request.identity.user.id
      organisationService.addOrganisationFlow(organisationIn, user) map {
        case -\/(error) =>
          Logger.error(error.toString)
            InternalServerError(Json.toJson("Error while creating group"))

        case \/-(organisation) => Ok(Json.toJson(organisation))
      }
    }

}
