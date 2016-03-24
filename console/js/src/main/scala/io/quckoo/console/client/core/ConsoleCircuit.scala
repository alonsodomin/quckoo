package io.quckoo.console.client.core

import diode._
import diode.react.ReactConnector
import io.quckoo.console.client.components.Notification
import io.quckoo.protocol.SchedulerProtocol

import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 20/02/2016.
  */
object ConsoleCircuit extends Circuit[ConsoleModel] with ReactConnector[ConsoleModel] {
  import SchedulerProtocol._

  protected def initialModel: ConsoleModel = ConsoleModel.initial

  override protected def actionHandler = combineHandlers(
    new LoginHandler(zoomRW(_.currentUser) { (model, value) => model.copy(currentUser = value) }),
    new RegistryHandler(zoomRW(_.notification)((m, notif) => m.copy(notification = notif))),
    new JobSpecsHandler(zoomRW(_.jobSpecs) { (model, specs) => model.copy(jobSpecs = specs) } ),
    scheduleHandler,
    new ExecutionPlanMapHandler(zoomRW(_.executionPlans) { (model, plans) => model.copy(executionPlans = plans) })
  )

  val scheduleHandler = new ActionHandler(zoomRW(_.notification)((m, notif) => m.copy(notification = notif))) {
    override def handle = {
      case msg: ScheduleJob =>
        updated(None, Effect(ConsoleClient.schedule(msg).map(_.fold(identity, identity))))

      case JobNotFound(jobId) =>
        updated(Some(Notification.danger(s"Job not found $jobId")))

      case ExecutionPlanStarted(jobId, planId) =>
        updated(Some(Notification.info(s"Started excution plan for job. planId=$planId")))
    }
  }

}
