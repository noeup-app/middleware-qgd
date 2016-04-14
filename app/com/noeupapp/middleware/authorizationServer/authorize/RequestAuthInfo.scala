package com.noeupapp.middleware.authorizationServer.authorize

case class RequestAuthInfo(clientId: String,
                           redirectUri: String,
                           scope: String,
                           state: String,
                           accepted: String )
