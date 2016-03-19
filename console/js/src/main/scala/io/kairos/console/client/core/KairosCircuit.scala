package io.kairos.console.client.core

import diode._
import diode.react.ReactConnector
import io.kairos.console.client.components.Notification
import io.kairos.protocol.SchedulerProtocol

import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 20/02/2016.
  */
object KairosCircuit extends Circuit[KairosModel] with ReactConnector[KairosModel] {
  import SchedulerProtocol._

  protected def initialModel: KairosModel = KairosModel.initial

  override protected def actionHandler = combineHandlers(
    new LoginHandler(zoomRW(_.currentUser) { (model, value) => model.copy(currentUser = value) }),
    new RegistryHandler(zoomRW(identity)((_, m) => m)),
    new JobSpecsHandler(zoomRW(_.jobSpecs) { (model, specs) => model.copy(jobSpecs = specs) } ),
    scheduleHandler
  )

  val scheduleHandler = new ActionHandler[KairosModel, KairosModel](zoomRW(identity)((_, m) => m)) {
    override def handle = {
      case msg: ScheduleJob =>
        updated(value.copy(notification = None), Effect(ClientApi.schedule(msg).map(_.fold(identity, identity))))

      case JobNotFound(jobId) =>
        updated(value.copy(notification = Some(Notification.danger(s"Job not found $jobId"))))

      case ExecutionPlanStarted(jobId, planId) =>
        updated(value.copy(notification = Some(Notification.info(s"Started excution plan for job"))))
    }
  }

}
