package io.quckoo.console.client.core

import diode.{ActionHandler, ActionResult}
import io.quckoo.auth.AuthInfo

/**
  * Created by alonsodomin on 26/03/2016.
  */
trait AuthHandler[S] { this: ActionHandler[ConsoleScope, S] =>

  def handleWithAuth(f: AuthInfo => ActionResult[ConsoleScope]): ActionResult[ConsoleScope] =
    this.modelRW.root.zoomMap(_.authInfo)(identity).value.map(f).getOrElse(ActionResult.NoChange)

}
