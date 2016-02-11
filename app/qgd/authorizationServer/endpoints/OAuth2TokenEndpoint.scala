package qgd.authorizationServer.endpoints

//import services.{ ImplicitGrantFlowHandler, SignatureHandler }
import scalaoauth2.provider._
//import utils.Config.{ GRANT_TYPE_SIGNATURE, GRANT_TYPE_TOKEN }

class OAuth2TokenEndpoint/*[U](signatureHandler: SignatureHandler[U], implicitGrantFlowHandler: ImplicitGrantFlowHandler[U])*/ extends TokenEndpoint {

  override val handlers = TokenEndpoint.handlers ++
    //Map(GRANT_TYPE_SIGNATURE -> signatureHandler) ++     // TODO TO BE MODIFIED
    //Map(GRANT_TYPE_TOKEN -> implicitGrantFlowHandler)++  // TODO Do we really want to use it?
    Map(
      OAuthGrantType.AUTHORIZATION_CODE -> new AuthorizationCode(),
      OAuthGrantType.REFRESH_TOKEN -> new RefreshToken(),
      OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials(),
      OAuthGrantType.PASSWORD -> new Password(),
      OAuthGrantType.IMPLICIT -> new Implicit()
    )
}