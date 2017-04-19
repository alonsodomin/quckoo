/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.console.core

import diode.{ActionProcessor, ActionResult, Dispatcher, Effect}

import io.quckoo._
import io.quckoo.console.components.Notification

import slogging.LazyLogging

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 22/09/2016.
  */
class ErrorProcessor extends ActionProcessor[ConsoleScope] with LazyLogging {

  import ActionResult._

  override def process(
      dispatch: Dispatcher,
      action: Any,
      next: (Any) => ActionResult[ConsoleScope],
      currentModel: ConsoleScope
  ): ActionResult[ConsoleScope] = {
    action match {
      case Failed(errors) =>
        val notifications = errors.map(generateNotification.lift).toList.flatMap(_.toList)
        if (notifications.isEmpty) NoChange
        else {
          val growlActions = notifications.map(Growl).map(Effect.action(_))
          EffectOnly(Effects.seq(growlActions.head, growlActions.tail: _*))
        }

      case _ => next(action)
    }
  }

  private[this] def generateNotification: PartialFunction[QuckooError, Notification] = {
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
