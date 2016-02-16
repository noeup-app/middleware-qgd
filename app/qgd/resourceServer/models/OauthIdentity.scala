package qgd.resourceServer.models

import java.util.UUID
import scala.language.postfixOps
import anorm.SqlParser._
import anorm._
import org.mindrot.jbcrypt.BCrypt
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.Json

/**
  * The user object. Password is never send with this object
  *
  * @param userID Required auto-generated unique ID of the user.
  * @param firstName Required first name of the authenticated user.
  * @param lastName Required last name of the authenticated user.
  * @param fullName Maybe the full name of the authenticated user.
  * @param email Maybe the email of the authenticated provider.
  * @param avatarURL Maybe the avatar URL of the authenticated provider.
  * @param deleted is user deleted
  */
case class OauthIdentity(
                        userID: UUID,
                        firstName: String,
                        lastName: String,
                        fullName: Option[String],
                        email: Option[String],    // TODO Need specific email scope to get email info
                        avatarURL: Option[String],
                        // salt: Option[String]
                        // createdAt: DateTime
                        deleted: Boolean
                        )

object OauthIdentity {

  implicit val oauthIdentityFormat = Json.format[OauthIdentity]

  val oauthIdentity =
    get[UUID]("user_id") ~
    get[String]("first_name") ~
    get[String]("last_name") ~
    get[Option[String]]("full_name") ~
    get[Option[String]]("email") ~
    get[Option[String]]("avatar_url") ~
    get[Boolean]("deleted") map {
      case id ~ firstName ~ lastName ~ fullName ~ email ~ avatar ~ deleted =>
        OauthIdentity(id, firstName, lastName, fullName, email, avatar, deleted)
    }

  /**
    * Encrypt the clear password using b-crypt
    *
    * @param password  the clear password to encrypt
    * @return          the hashed password and the salt used
    */
  def encryptPassword(password: String): (String, Option[String]) = {
    val salt = BCrypt.gensalt(10)
    val hash = BCrypt.hashpw(password, salt)
    (hash, Some(salt))
  }


  /**
    * @param email unique email
    * @param encryptedPassword Encrypted version of password
    * @return Option oauthIdentity.
    */
  def findByEmailAndPassword(email: String, encryptedPassword: String):Option[OauthIdentity] = DB.withConnection(implicit c =>
        SQL(
          """
          SELECT *
          FROM oauth_identity i
          WHERE i.email = {email} AND i.password = {password}
          """)
          .on("email" -> email,
            "password" -> encryptedPassword)
          .as(oauthIdentity *).headOption
      )

  /**
    * @param id user id
    * @return Option oauthIdentity.
    */
  def findByUserId(id: UUID):Option[OauthIdentity] = DB.withConnection(implicit c =>
    SQL(
      """
          SELECT *
          FROM oauth_identity i
          WHERE i.client_id = {client_id}
      """)
      .on("client_id" -> id)
      .as(oauthIdentity *).headOption
  )


  /**
    * @param user User object
    * @param password encrypted password
    * @return
    */
  def insert(user: OauthIdentity, password: String) = DB.withConnection(implicit c =>
    SQL("""
          INSERT INTO oauth_identity
            (user_id, first_name, last_name, full_name, password, email, avatar_url)
          VALUES
            ({user_id}, {first_name}, {last_name}, {full_name},{paswword} , {email}, {avatar_url})
        """)
      .on("user_id" -> user.userID,
        "first_name"-> user.firstName,
        "last_name" -> user.lastName,
        "full_name" -> user.fullName,
        "email" -> user.email,
        "password" -> password,
        "avatar_url" -> user.avatarURL)
      .execute()
  )


  /**
    * Delete the the given user from database.
    *
    * @param userId
    * @return
    */
  def delete(userId: UUID) = DB.withConnection( implicit c =>
    SQL("""
      DELETE FROM oauth_identity
      WHERE user_id = {user_id}
        """)
      .on(
        "user_id" -> userId
      ).execute()
  )


  /**
  * Update the given client.
  *
  * @param userId
  * @return
  */
  def update(userId: UUID) =  DB.withConnection( implicit c =>
    SQL("""
      UPDATE auth_clients
      SET client_id = {client_id},
          client_name = {client_name},
          client_secret = {client_secret},
          description = {description},
          redirect_uri = {redirect_uri},
          scope = {scope}
      WHERE client_id = {client_id}
        """)
      .on(
        "client_id" -> userId)
      .execute()
  )

}
