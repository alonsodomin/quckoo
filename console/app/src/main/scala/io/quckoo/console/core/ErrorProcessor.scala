package io.quckoo.console.core


import diode.{ActionProcessor, ActionResult, Dispatcher, Effect}

import io.quckoo.console.components.Notification
import io.quckoo.fault._

import slogging.LazyLogging

import scalajs.concurrent.JSExecutionContext.Implicits.queue

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 22/09/2016.
  */
class ErrorProcessor extends ActionProcessor[ConsoleScope] with LazyLogging {

  import ActionResult._

  override def process(
    dispatch: Dispatcher, action: Any,
    next: (Any) => ActionResult[ConsoleScope],
    currentModel: ConsoleScope
  ): ActionResult[ConsoleScope] = {
    action match {
      case Failed(errors) =>
        val notifications = errors.map(generateNotification.lift).toList.flatMap(_.toList)
        if (notifications.isEmpty) NoChange
        else {
          val growlActions = notifications.
            map(Growl).map(Effect.action(_))
          EffectOnly(Effects.seq(growlActions.head, growlActions.tail: _*))
        }

      case _ => next(action)
    }
  }

  private[this] def generateNotification: PartialFunction[Fault, Notification] ={
    case JobNotFound(jobId) =>
      Notification.danger(s"Job not found: $jobId")

    case JobNotEnabled(jobId) =>
      Notification.warning(s"Job not enabled: $jobId")

    case ExecutionPlanNotFound(planId) =>
      Notification.danger(s"Execution plan not found: $planId")

    case TaskExecutionNotFound(taskId) =>
      Notification.danger(s"Task execution not found: $taskId")
  }

}
