package io.quckoo.console.client.core

import diode.{ActionProcessor, ActionResult, Dispatcher}
import io.quckoo.console.client.SiteMap
import io.quckoo.console.client.SiteMap.ConsoleRoute
import japgolly.scalajs.react.extra.router.RouterCtl

/**
  * Created by alonsodomin on 26/03/2016.
  */
class LoginProcessor(routerCtl: RouterCtl[ConsoleRoute]) extends ActionProcessor[ConsoleScope] {

  override def process(dispatch: Dispatcher, action: AnyRef,
                       next: (AnyRef) => ActionResult[ConsoleScope],
                       currentModel: ConsoleScope): ActionResult[ConsoleScope] = {
    action match {
      case LoggedOut =>
        routerCtl.set(SiteMap.LoginRoute).runNow()
        ActionResult.ModelUpdate(currentModel.copy(authInfo = None))

      case _ =>
        val result = next(action)
        next(action)
    }
  }

}
