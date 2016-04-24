package com.noeupapp.middleware.entities.organisation

import java.util.UUID

import com.google.inject.Inject
import com.noeupapp.middleware.errorHandle.ExceptionEither._
import com.noeupapp.middleware.errorHandle.FailError
import com.noeupapp.middleware.errorHandle.FailError.Expect

import scala.concurrent.Future
import scalaz.{-\/, \/-}


class OrganisationService @Inject() (organisationDAO: OrganisationDAO){
  def fetchOrganisation(organisationId: UUID): Future[Expect[Organisation]] =
    TryBDCall({ implicit c =>
      // TODO : limit result to own processes
      // var list = processDAO.getByAdmin()
      organisationDAO.findById(organisationId) match{
        case Some(process) => \/-(process)
        case None => -\/(FailError("Cannot find the organisation"))
      }
    })
}
