package io.quckoo.console.core

import diode.{ActionProcessor, ActionResult, Dispatcher}
import io.quckoo.console.SiteMap.{ConsoleRoute, DashboardRoute, LoginRoute}
import io.quckoo.console.components.Notification
import japgolly.scalajs.react.extra.router.RouterCtl

/**
  * Created by alonsodomin on 26/03/2016.
  */
class LoginProcessor(routerCtl: RouterCtl[ConsoleRoute]) extends ActionProcessor[ConsoleScope] {

  val authFailedNotification = Notification.danger("Username or password incorrect")

  override def process(dispatch: Dispatcher, action: AnyRef,
                       next: (AnyRef) => ActionResult[ConsoleScope],
                       currentModel: ConsoleScope): ActionResult[ConsoleScope] = {
    action match {
      case LoginFailed =>
        ActionResult.ModelUpdate(currentModel.copy(notification = Some(authFailedNotification)))

      case LoggedIn(client, referral) =>
        routerCtl.set(referral.getOrElse(DashboardRoute)).runNow()
        ActionResult.ModelUpdate(currentModel.copy(client = Some(client), notification = None))

      case LoggedOut =>
        routerCtl.set(LoginRoute).runNow()
        ActionResult.ModelUpdate(currentModel.copy(client = None))

      case _ =>
        next(action)
    }
  }

}
