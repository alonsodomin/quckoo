package io.kairos.console.client.core

import diode.Circuit
import diode.react.ReactConnector

/**
  * Created by alonsodomin on 20/02/2016.
  */
object KairosCircuit extends Circuit[KairosModel] with ReactConnector[KairosModel] {
  protected def initialModel: KairosModel = KairosModel.initial

  override protected def actionHandler = combineHandlers(
    new LoginHandler(zoomRW(_.currentUser) { (model, value) => model.copy(currentUser = value) })
  )
}