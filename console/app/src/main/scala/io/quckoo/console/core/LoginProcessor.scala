package io.quckoo.console.core

import diode._
import io.quckoo.console.ConsoleRoute
import io.quckoo.console.components.Notification
import japgolly.scalajs.react.extra.router.RouterCtl

import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 26/03/2016.
  */
class LoginProcessor(routerCtl: RouterCtl[ConsoleRoute]) extends ActionProcessor[ConsoleScope] {
  import ConsoleRoute._

  val authFailedNotification = Notification.danger("Username or password incorrect")

  override def process(dispatch: Dispatcher, action: AnyRef,
                       next: (AnyRef) => ActionResult[ConsoleScope],
                       currentModel: ConsoleScope): ActionResult[ConsoleScope] = {
    action match {
      case LoginFailed =>
        ActionResult.ModelUpdate(currentModel.copy(notification = Some(authFailedNotification)))

      case LoggedIn(client, referral) =>
        val navigate = Effect.action(NavigateTo(referral.getOrElse(DashboardRoute)))
        val subscribe = Effect.action(SubscribeToBackend(client))

        ActionResult.ModelUpdateEffect(
          currentModel.copy(client = Some(client), notification = None),
          subscribe >> navigate
        )

      case LoggedOut =>
        val action = Effect.action(NavigateTo(DashboardRoute))
        ActionResult.ModelUpdateEffect(currentModel.copy(client = None, notification = None), action)

      case NavigateTo(route) =>
        routerCtl.set(route).runNow()
        ActionResult.NoChange

      case _ =>
        next(action)
    }
  }

}
