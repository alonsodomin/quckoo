package io.kairos.console.client.core

import diode._
import diode.react.ReactConnector

/**
  * Created by alonsodomin on 20/02/2016.
  */
object KairosCircuit extends Circuit[KairosModel] with ReactConnector[KairosModel] {
  protected def initialModel: KairosModel = KairosModel.initial(this)

  override protected def actionHandler = combineHandlers(
    new LoginHandler(zoomRW(_.currentUser) { (model, value) => model.copy(currentUser = value) }),
    new RegistryHandler(this, zoomRW(_.jobSpecs) { (model, value) => model.copy(jobSpecs = value) } )
  )

}
