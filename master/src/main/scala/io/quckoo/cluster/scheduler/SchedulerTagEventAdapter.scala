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

package io.quckoo.cluster.scheduler

import akka.persistence.journal.{Tagged, WriteEventAdapter}

import io.quckoo.protocol.scheduler._

import slogging._

/**
  * Created by alonsodomin on 13/03/2016.
  */
class SchedulerTagEventAdapter extends WriteEventAdapter with StrictLogging {
  import SchedulerTagEventAdapter._

  logger.debug("Scheduler event adapter initialized.")

  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case evt: ExecutionDriver.Initialized => Tagged(evt, Set(tags.ExecutionPlan))
    case evt: ExecutionPlanStarted        => Tagged(evt, Set(tags.ExecutionPlan))
    case evt: ExecutionPlanFinished       => Tagged(evt, Set(tags.ExecutionPlan))
    case evt: TaskScheduled               => Tagged(evt, Set(tags.Task))
    case evt: TaskTriggered               => Tagged(evt, Set(tags.Task))
    case evt: TaskCompleted               => Tagged(evt, Set(tags.Task))

    case _ => event
  }

}

object SchedulerTagEventAdapter {

  object tags {
    final val ExecutionPlan = "execution-plan"
    final val Task          = "task"
  }

}
