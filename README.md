QGD Authentication and Authorization module
===================
QGD Authentication and Authorization module allows you to start a secured **Play2 scala** API project out of the box.  

QGD Authentication and Authorization module in respect  with [RCF6749](http://tools.ietf.org/html/rfc6749) is based on following modules :


| Role     | Plugin / Module | Version   |
| :------- | :----           | :---:      |
| Authorization server |  [scala-oauth2-provider](https://github.com/nulab/scala-oauth2-provider)  |  (0.16.x)|
| Authorization client |  [securesocial](https://github.com/jaliss/securesocial)  |  2|
| Resource Server | *[your own API]* |

Ready to use 
------
###scala-oauth2-provider  extended with preconfigured: 
**1. DB model**
Postgresql schema for data persistance
> TODO  : Think about using memcache or redis.

**2. mydatahandler**
```
def validateClient(clientId: String, clientSecret: String, grantType: String): Boolean 
def findUser(username: String, password: String): Option[User]
def createAccessToken(authInfo: AuthInfo[User]): scalaoauth2.provider.AccessToken 
def getStoredAccessToken(authInfo: AuthInfo[User]): Option[scalaoauth2.provider.AccessToken]
def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): scalaoauth2.provider.AccessToken
def findAuthInfoByCode(code: String): Option[AuthInfo[User]]
def findAuthInfoByRefreshToken(refreshToken: String): Option[AuthInfo[User]]
def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Option[User]
def findAccessToken(token: String): Option[scalaoauth2.provider.AccessToken] 
def findAuthInfoByAccessToken(accessToken: scalaoauth2.provider.AccessToken): Option[AuthInfo[User]]
```
**3. Auth User**
```
case class User(
	id: Option[Int], 
	username: String,
	email: String,
	password: String, 
	role: String
	)
```
###scala-oauth2-provider  with new fonctionnality: 

**1. Manage clients**
Add, delete, generates code
 

####securesocial 
Also works with your own Auth2 provider. 