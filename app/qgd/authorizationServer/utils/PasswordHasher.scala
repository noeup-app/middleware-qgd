package qgd.authorizationServer.utils

import org.mindrot.jbcrypt._

/**
  * The password details
  *
  * @param hasher the id of the hasher used to hash this password
  * @param password the hashed password
  * @param salt the optional salt used when hashing
  */
case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None)


/**
  * A trait that defines the password hasher interface
  */

abstract class PasswordHasher {
  /**
    * The password hasher id
    */
  val id: String

  /**
    * Hashes a password
    *
    * @param plainPassword the password to hash
    * @return a PasswordInfo containting the hashed password and optional salt
    */
  def hash(plainPassword: String): PasswordInfo

  /**
    * Checks whether a supplied password matches the hashed one
    *
    * @param passwordInfo the password retrieved from the backing store (by means of UserService)
    * @param suppliedPassword the password supplied by the user trying to log in
    * @return true if the password matches, false otherwise.
    */
  def matches(passwordInfo: PasswordInfo, suppliedPassword: String): Boolean
}

object PasswordHasher {
  val id = "bcrypt"

  /**
    * The default password hasher based on BCrypt.
    */
  class Default(logRounds: Int) extends PasswordHasher {
    /**
      * Creates an instance with logRounds set to the value specified in
      * securesocial.passwordHasher.bcrypt.rounds or to a default 10 if the property is not
      * defined.
      */
    def this() = this({
      val app = play.api.Play.current
      app.configuration.getInt(Default.RoundsProperty).getOrElse(Default.Rounds)
    })

    /**
      * The hasher id
      */
    override val id = PasswordHasher.id

    /**
      * Hashes a password. This implementation does not return the salt because it is not needed
      * to verify passwords later.  Other implementations might need to return it so it gets saved in the
      * backing store.
      *
      * @param plainPassword the password to hash
      * @return a PasswordInfo containing the hashed password.
      */
    def hash(plainPassword: String): PasswordInfo = {
      PasswordInfo(id, BCrypt.hashpw(plainPassword, BCrypt.gensalt(logRounds)))
    }

    /**
      * Checks if a password matches the hashed version
      *
      * @param passwordInfo the password retrieved from the backing store (by means of UserService)
      * @param suppliedPassword the password supplied by the user trying to log in
      * @return true if the password matches, false otherwise.
      */
    def matches(passwordInfo: PasswordInfo, suppliedPassword: String): Boolean = {
      BCrypt.checkpw(suppliedPassword, passwordInfo.password)
    }
  }

  object Default {
    val Rounds = 10
    val RoundsProperty = "securesocial.passwordHasher.bcrypt.rounds"
  }
}