//package qgd.resourceServer.models
//
//import java.util.UUID
//import scala.language.postfixOps
//import anorm.SqlParser._
//import anorm._
//import org.mindrot.jbcrypt.BCrypt
//import play.api.Play.current
//import play.api.db.DB
//import play.api.libs.json.Json
//
///**
//  * The user object. Password is never send with this object
//  *
//  * @param userID Required auto-generated unique ID of the user.
//  * @param firstName Required first name of the authenticated user.
//  * @param lastName Required last name of the authenticated user.
//  * @param fullName Maybe the full name of the authenticated user.
//  * @param email Maybe the email of the authenticated provider.
//  * @param avatarURL Maybe the avatar URL of the authenticated provider.
//  * @param deleted is user deleted
//  */
//case class OauthIdentity(
//                        userID: UUID,
//                        firstName: String,
//                        lastName: String,
//                        fullName: Option[String],
//                        email: Option[String],    // TODO Need specific email scope to get email info
//                        avatarURL: Option[String],
//                        // salt: Option[String]
//                        // createdAt: DateTime
//                        deleted: Boolean
//                        )
//
//object OauthIdentity {
//
//  implicit val oauthIdentityFormat = Json.format[OauthIdentity]
//
//  val oauthIdentity =
//    get[UUID]("user_id") ~
//    get[String]("first_name") ~
//    get[String]("last_name") ~
//    get[Option[String]]("full_name") ~
//    get[Option[String]]("email") ~
//    get[Option[String]]("avatar_url") ~
//    get[Boolean]("deleted") map {
//      case id ~ firstName ~ lastName ~ fullName ~ email ~ avatar ~ deleted =>
//        OauthIdentity(id, firstName, lastName, fullName, email, avatar, deleted)
//    }
//
//  /**
//    * Encrypt the clear password using b-crypt
//    *
//    * @param password  the clear password to encrypt
//    * @return          the hashed password and the salt used
//    */
//  def encryptPassword(password: String): (String, Option[String]) = {
//    val salt = BCrypt.gensalt(10)
//    val hash = BCrypt.hashpw(password, salt)
//    (hash, Some(salt))
//  }
//
//
//  /**
//    * @param email unique email
//    * @param encryptedPassword Encrypted version of password
//    * @return Option oauthIdentity.
//    */
//  def findByEmailAndPassword(email: String, encryptedPassword: String):Option[OauthIdentity] = DB.withConnection(implicit c =>
//        SQL(
//          """
//          SELECT *
//          FROM oauth_identity i
//          WHERE i.email = {email} AND i.password = {password}
//          """)
//          .on("email" -> email,
//            "password" -> encryptedPassword)
//          .as(oauthIdentity *).headOption
//      )
//
//  /**
//    * @param id user id
//    * @return Option oauthIdentity.
//    */
//  def findByUserId(id: UUID):Option[OauthIdentity] = DB.withConnection(implicit c =>
//    SQL(
//      """
//          SELECT *
//          FROM oauth_identity i
//          WHERE i.client_id = {client_id}
//      """)
//      .on("client_id" -> id)
//      .as(oauthIdentity *).headOption
//  )
//
//
//  /**
//    * @param user User object
//    * @param password encrypted password
//    * @return
//    */
//  def insert(user: OauthIdentity, password: String) = DB.withConnection(implicit c =>
//    SQL("""
//          INSERT INTO oauth_identity
//            (user_id, first_name, last_name, full_name, password, email, avatar_url)
//          VALUES
//            ({user_id}, {first_name}, {last_name}, {full_name},{paswword} , {email}, {avatar_url})
//        """)
//      .on("user_id" -> user.userID,
//        "first_name"-> user.firstName,
//        "last_name" -> user.lastName,
//        "full_name" -> user.fullName,
//        "email" -> user.email,
//        "password" -> password,
//        "avatar_url" -> user.avatarURL)
//      .execute()
//  )
//
//
//  /**
//    * Delete the the given user from database.
//    *
//    * @param userId
//    * @return
//    */
//  def delete(userId: UUID) = DB.withConnection( implicit c =>
//    SQL("""
//      DELETE FROM oauth_identity
//      WHERE user_id = {user_id}
//        """)
//      .on(
//        "user_id" -> userId
//      ).execute()
//  )
//
//
//  /**
//  * Update the given client.
//  *
//  * @param userId
//  * @return
//  */
//  def update(userId: UUID) =  DB.withConnection( implicit c =>
//    SQL("""
//      UPDATE auth_clients
//      SET client_id = {client_id},
//          client_name = {client_name},
//          client_secret = {client_secret},
//          description = {description},
//          redirect_uri = {redirect_uri},
//          scope = {scope}
//      WHERE client_id = {client_id}
//        """)
//      .on(
//        "client_id" -> userId)
//      .execute()
//  )
//
//}
//
///*package controllers
//
//import java.util.UUID
//import controllers.Application._
//import org.joda.time.DateTime
//
//import models._
//import org.mindrot.jbcrypt.BCrypt
//import play.api._
//import play.api.db.slick._
//import play.api.db.slick.Config.driver.simple._
//import play.api.data._
//import play.api.data.Forms._
//import play.api.mvc._
//import play.api.Play.current
//import play.api.mvc.BodyParsers._
//import play.api.libs.json._
//import play.api.libs.json.Json._
//import play.api.libs.functional.syntax._
//import play.api.libs.concurrent.Execution.Implicits._
//
//import dao.Tables.Users
//
//object UserController extends Controller {
//
//  //JSON read/write macro
//  implicit val userFormat = Json.format[User]
//
//  //  case class CreateUser2(email: String, password: String, firstName: Option[String], lastName: Option[String])
//  //
//  //  implicit def createUserReads2: Reads[CreateUser2] = {
//  //    (
//  //      (__ \ "email").read[String] and
//  //        (__ \ "password").read[String] and
//  //        (__ \ "firstName").readNullable[String] and
//  //        (__ \ "lastName").readNullable[String]
//  //      )(CreateUser2.apply _)
//  //  }
//  //
//  //  implicit def createUserWrites2: play.api.libs.json.Writes[CreateUser2] = {
//  //    (
//  //      (__ \ "email").write[String] and
//  //        (__ \ "password").write[String] and
//  //        (__ \ "firstName").write[Option[String]] and
//  //        (__ \ "lastName").write[Option[String]]
//  //      )(unlift(CreateUser2.unapply _))
//  //  }
//
//  case class CreateUser(email: String, password: String, firstName: String, lastName: String)
//
//  implicit def createUserReads: Reads[CreateUser] = {
//    (
//      (__ \ "email").read[String] and
//        (__ \ "password").read[String] and
//        (__ \ "firstName").read[String] and
//        (__ \ "lastName").read[String]
//      )(CreateUser.apply _)
//  }
//
//  implicit def createUserWrites: play.api.libs.json.Writes[CreateUser] = {
//    (
//      (__ \ "email").write[String] and
//        (__ \ "password").write[String] and
//        (__ \ "firstName").write[String] and
//        (__ \ "lastName").write[String]
//      )(unlift(CreateUser.unapply _))
//  }
//
//  def jsonInsert = DBAction(parse.json) { implicit request =>
//
//    //implicit val createUserFormat = Json.format[CreateUser]
//    //val user = request.body.validate[CreateUser]
//
//    request.request.body.validate[CreateUser].map { createdUser =>
//      saveUser(createdUser)
//
//      Ok(toJson(createdUser))
//    }.getOrElse(BadRequest("invalid json"))
//  }
//
//  /**
//    * Encrypt the clear password using b-crypt
//    *
//    * @param password  the clear password to encrypt
//    * @return          the hashed password and the salt used
//    */
//  def encryptPassword(password: String): (String, Option[String]) = {
//    val salt = BCrypt.gensalt(10)
//    val hash = BCrypt.hashpw(password, salt)
//    (hash, Some(salt))
//  }
//
//  def jsonFindAll = DBAction { implicit rs =>
//    Ok(toJson(Users.list))
//  }
//
//  def saveUser(user : CreateUser) = {
//    val (hash, salt) = encryptPassword(user.password)
//    val now = DateTime.now
//    val uuid = UUID.randomUUID
//    val u = User(java.util.UUID.randomUUID, user.firstName, user.lastName, user.email, hash, salt, now, uuid, now, uuid, None, None)
//
//    dao.psql.UserDAOPsql.createUser(u)
//
//  }
//}*/
//
///*
//import play.api.db.slick.Config.driver.simple._
//import slick.model.Table
//import scala.slick.lifted.Tag
//
//case class Identity(id: Option[Int],
//                    username: String,
//                    email: String,
//                    password: String,
//                    role: String)
//
//class Identity(tag: Tag) extends Table[Identity](tag, "users") {
//  def id = column[Int]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
//  def username = column[String]("username", O.NotNull)
//  def email = column[String]("email", O.NotNull)
//  def password = column[String]("password", O.NotNull)
//  def role = column[String]("role", O.NotNull)
//  def * = (id.?, username, email, password, role) <> (Identity.tupled, Identity.unapply _)
//}
//
//object Users {
//  val users = TableQuery[Users]
//
//  def get(id: Int)(implicit session: Session): Option[Identity] =
//    users.where(_.id === id).firstOption
//
//  def findByUsername(username: String)(implicit session: Session): Option[Identity] =
//    users.where(_.username === username).firstOption
//
//  /**
//    * @param username Username to find
//    * @param encryptedPassword Encrypted version of password
//    * @param session Implicit database session
//    * @return Option containing User.
//    */
//  def findByUsernameAndPassword(username: String, encryptedPassword: String)(implicit session: Session): Option[Identity] = {
//    users.where(user =>
//      user.username === username && user.password === encryptedPassword).firstOption
//  }
//
//  def autoInc = users returning users.map(_.id)
//
//  /**
//    * @param user User object with already encrypted password
//    * @param session
//    * @return
//    */
//  def insert(user: Identity)(implicit session: Session) = {
//    val encUser = Identity(user.id, user.username, user.email, user.password, user.role)
//    encUser.id match {
//      case None => autoInc += encUser
//      case Some(x) => users += encUser
//    }
//  }
//
//  /**
//    * @param id User id to be updated
//    * @param user New User details
//    * @param session Implicit database session
//    * @return
//    */
//  def update(id: Int, user: Identity)(implicit session: Session) =
//    users.where(_.id === user.id).update(user)
//
//  /**
//    * @param user User object to be deleted
//    * @param session Implicit database session
//    * @return
//    */
//  def delete(user: Identity)(implicit session: Session) =
//    users.where(_.id === user.id).delete
//
//  /**
//    * Delete all the users. NOTE: Use with caution.
//    *
//    * @param session Implicit database session
//    * @return
//    */
//  def deleteAll()(implicit session: Session) = users.delete
//
//}*/
