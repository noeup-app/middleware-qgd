package com.noeupapp.middleware.authorizationClient.loginInfo

import java.sql.Connection
import java.util.UUID

import anorm.SQL
import com.noeupapp.middleware.entities.entity.Entity

import scala.language.postfixOps

/**
  * Created by damien on 23/02/2017.
  */
class AuthLoginInfoDao {


  def find(providerId: String, providerKey: String)(implicit connection: Connection): Option[AuthLoginInfo] = {
    SQL(
      """SELECT *
         FROM auth_login_info
         WHERE provider_id = {provider_id} AND provider_key = {provider_key};""")
      .on(
        'provider_id -> providerId,
        'provider_key -> providerKey
      ).as(AuthLoginInfo.parse *).headOption
  }


  def add(authLoginInfo: AuthLoginInfo)(implicit connection: Connection) =
    SQL(
      """
          INSERT INTO auth_login_info (provider_id, provider_key, "user")
          VALUES ({provider_id},
                  {provider_key},
                  {user}::UUID)
      """
    ).on(
      'provider_id -> authLoginInfo.providerId,
      'provider_key -> authLoginInfo.providerKey,
      'user -> authLoginInfo.user
    ).execute()


  def update(providerId: String, providerKey: String, authLoginInfo: AuthLoginInfo)(implicit connection: Connection) =
    SQL(
      """
          UPDATE auth_login_info
          SET provider_id = {new_provider_id},
              provider_key = {new_provider_key},
              "user" = {new_user}::UUID
          WHERE provider_id = {provider_id} AND provider_key = {provider_key};
      """
    ).on(
      'provider_id -> providerId,
      'provider_key -> providerKey,
      'new_provider_id -> authLoginInfo.providerId,
      'new_provider_key -> authLoginInfo.providerKey,
      'new_user -> authLoginInfo.user
    ).execute()


  def delete(userId: UUID)(implicit connection: Connection) = {
    SQL(
      """
          DELETE FROM auth_login_info
          WHERE "user" = {user}::UUID
      """
    ).on(
      'user -> userId
    ).executeUpdate()
  }



}
