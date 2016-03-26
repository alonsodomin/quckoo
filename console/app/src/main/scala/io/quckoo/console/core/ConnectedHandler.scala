package io.quckoo.console.core

import diode.{ActionHandler, ActionResult}
import io.quckoo.client.QuckooClient

/**
  * Created by alonsodomin on 26/03/2016.
  */
private[core] trait ConnectedHandler[S] { this: ActionHandler[ConsoleScope, S] =>

  def withClient(f: QuckooClient => ActionResult[ConsoleScope]): ActionResult[ConsoleScope] =
    this.modelRW.root.zoomMap(_.client)(identity).value.map(f).getOrElse(ActionResult.NoChange)

}
