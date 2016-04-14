package qgd.middleware.authorizationServer.models

case class RequestAuthInfo(clientId: String,
                           redirectUri: String,
                           scope: String,
                           state: String,
                           accepted: String )
