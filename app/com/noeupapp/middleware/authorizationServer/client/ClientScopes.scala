package com.noeupapp.middleware.authorizationServer.client

case class ClientScopes(clientId: String, scopes: Option[List[String]] = None)