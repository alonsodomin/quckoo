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

package io.quckoo

import io.quckoo.id._

import monocle.macros.Lenses

import org.threeten.bp.{Clock, ZonedDateTime}

/**
  * Created by alonsodomin on 14/03/2016.
  */
@Lenses final case class ExecutionPlan(
    jobId: JobId,
    planId: PlanId,
    trigger: Trigger,
    createdTime: ZonedDateTime,
    currentTask: Option[Task] = None,
    lastOutcome: Option[TaskExecution.Outcome] = None,
    lastTriggeredTime: Option[ZonedDateTime] = None,
    lastScheduledTime: Option[ZonedDateTime] = None,
    lastExecutionTime: Option[ZonedDateTime] = None,
    finishedTime: Option[ZonedDateTime] = None
) {

  def finished: Boolean = finishedTime.isDefined

  def nextExecutionTime(implicit clock: Clock): Option[ZonedDateTime] = {
    import Trigger.{LastExecutionTime, ScheduledTime}

    val referenceTime = lastExecutionTime match {
      case Some(time) => LastExecutionTime(time)
      case None       => ScheduledTime(lastScheduledTime.getOrElse(createdTime))
    }
    trigger.nextExecutionTime(referenceTime)
  }

}
