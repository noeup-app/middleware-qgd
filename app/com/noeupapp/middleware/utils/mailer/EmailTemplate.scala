package com.noeupapp.middleware.utils.mailer

/**
  * Trait to custom emails
  */
trait EmailTemplate {
  //Common data
  def getSenderName: String

  def getSenderEmail: String

  def getAppName: String

  def getAppLink: String

  def getLogoUrl: String

  def getCompanyName: String

  def getCompanyEmail: String


  //Forgot PWD data

  def getForgotPwdSubject: String

  def getForgotPwdContent(token :String): String

  //Email confirmation data

  def getEmailConfirmSubject: String
}