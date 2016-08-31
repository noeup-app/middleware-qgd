package com.noeupapp.middleware.entities.organisation

import java.util.UUID

import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.utils.FutureFunctor._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect
import com.noeupapp.middleware.entities.group.{Group, GroupService}
import com.noeupapp.middleware.entities.entity.EntityService

import scala.concurrent.Future
import scalaz.{-\/, EitherT, \/-}
import scala.concurrent.ExecutionContext.Implicits.global


class OrganisationService @Inject() (organisationDAO: OrganisationDAO,
                                     groupService: GroupService,
                                     entityService: EntityService){
  def fetchOrganisation(organisationId: UUID): Future[Expect[Option[Organisation]]] =
    TryBDCall{ implicit c =>
      // TODO : limit result to own processes
      // var list = processDAO.getByAdmin()
      organisationDAO.findById(organisationId) match{
        case res @ Some(_) => \/-(res)
        case None =>  \/-(None)
      }
    }

  def addOrganisationFlow(organisationInput: OrganisationIn, userId: UUID): Future[Expect[Organisation]] = {
    for {
      organisation <- EitherT(addOrganisation(Organisation(UUID.randomUUID(),
        organisationInput.name,
        organisationInput.sub_domain,
        organisationInput.logo_url,
        organisationInput.color,
        organisationInput.credits,
        deleted = false)))

      adminGroup <- EitherT(groupService.addGroup(Group(UUID.randomUUID(),
                                                        "Admin",
                                                        userId)))
      hierarchy <- EitherT(entityService.addHierarchy(organisation.id, adminGroup.id))
    } yield organisation
  }.run

  def addOrganisation(organisation: Organisation): Future[Expect[Organisation]] = {
    TryBDCall { implicit c =>
      organisationDAO.add(organisation)
      \/-(organisation)
    }
  }
}
