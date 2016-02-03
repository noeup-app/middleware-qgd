package qgd.authorizationClient.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import qgd.authorizationClient.models.User
import play.api.i18n.MessagesApi
import qgd.authorizationClient.utils.{WithScopes, WithScope}


class TestScopeController @Inject() (
                                      val messagesApi: MessagesApi,
                                      val env: Environment[User, CookieAuthenticator])
  extends Silhouette[User, CookieAuthenticator] {

  /**
    * Nominal case : no scope
    */
  def testNoScopeRequired = SecuredAction {
    Ok("access provided")
  }

  /**
    * Scope admin
    */
  def testScopeAdminRequired = SecuredAction(WithScope("admin")) {
    Ok("access provided")
  }

  /**
    * Scope admin
    */
  def testScopeBuyRequired = SecuredAction(WithScope("buy")) {
    Ok("access provided")
  }

  /**
    * Scope admin OR buy
    */
  def testScopeAdminOrBuyRequired = SecuredAction(WithScope("admin", "buy")) {
    Ok("access provided")
  }

  /**
    * Scope admin AND buy
    */
  def testScopeAdminAndBuyRequired = SecuredAction(WithScopes("admin", "buy")) {
    Ok("access provided")
  }

}