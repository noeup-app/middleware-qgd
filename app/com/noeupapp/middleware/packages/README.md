# Package module
This module helps to set up business packages and manage credit.



## How to use it
### In Services
```
// inject: `packageHandler: PackageHandler`

packageHandler.isAuthorized(the_user, Action.actionName)
```

### In controllers
Can be used only with `SecuredAction` and `UserAwareAction`.
I make no sense to use it `Action` because we can track user.

First inject `checkPackageAction: CheckPackageAction`

#### Secured Action
```
def index = (SecuredAction andThen checkPackageAction.checkSecured(this)) { implicit request =>
    Ok("")
  }
```
#### User aware action
```
def index = (UserAwareAction andThen checkPackageAction.checkUserAware(this)) { implicit request =>
    Ok("")
  }
```









## How to implement it
This implementation has to be done in the project (not in middle).


### Evolution
Call `/evolution/2`.

### Binding in module (Guice)
```
    bind[PackageHandler].to[PackageHandlerImpl]
```

### PackageState
PackageState define the credit the user can use. For instance, user can create 5 other users and sent 100 emails.

Example of implementation :
```
case class PackageState(numberOfUsers: Int) {
  def userPlusOne = PackageState(numberOfUsers + 1)
  def userLessOne = PackageState(numberOfUsers - 1)
}


object PackageState {

  implicit val packageStateFormat = Json.format[PackageState]

}
```


### Action (not mandatory but recommended)
I advise to create an object for all actions. Something like this.

```
object Action {

  val addRNJ = "addRNJ"
  val deleteRNJ = "deleteRNJ"

}
```


### ActionPackageImpl
Association between an action an it cost.

```
class ActionCostImpl extends ActionCost {

  override def cost(action: String): Int = {
    action match {
      case Action.addRNJ => 1
      case Action.deleteRNJ => 1
    }
  }
}

```


### PackageHandlerImpl

Note that you can use `mapPackageState` (defined in `PackageHandler`) to :
- first, check if user has enough credit to do an action
- then, to update the state of the package linked to the user

Example of implementation : 
```
class PackageHandlerImpl @Inject()(/* ... */) extends PackageHandler {

    override type PackageState = <PackageState class>

    override def jsonProcess(actionName: String, user: User, usersPackages: Set[Pack], relEntPack: RelationEntityPackage): Future[Expect[Unit]] = {
        val stateUpdater = mapPackageState(relEntPack)(_)

        val checkMoreThanOne: Option[PackageState] => Option[String] = {
          case Some(packageState) if packageState.numberOfUsers > 0 => None
          case Some(_) => Some("You don't have enough credit")
          case _ => Some("Package is not defined")
        }

        actionName match {

          case Action.addRNJ =>
            stateUpdater(checkMoreThanOne)(_.map(_.userLessOne))

          case Action.deleteRNJ =>
            stateUpdater(checkMoreThanOne)(_.map(_.userPlusOne))

        }
      }

      override protected def mapHttpToActionName(httpMethod: String, withOutHttpContext: String): String = httpMethod -> withOutHttpContext match {
        case (GET, "/") => Action.addRNJ
      }


}
```

