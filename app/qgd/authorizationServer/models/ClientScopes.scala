package qgd.authorizationServer.models

case class ClientScopes(clientId: String, scopes: Option[List[String]] = None)