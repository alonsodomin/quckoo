/*
 * Copyright 2015 A. Alonso Dominguez
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

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern._
import akka.util.Timeout

import cats.Eval
import cats.effect.{LiftIO, IO}
import cats.implicits._

import io.quckoo._
import io.quckoo.api2.{QuckooIO, Scheduler => SchedulerAPI}
import io.quckoo.protocol.scheduler._

import scala.concurrent.duration._

class SchedulerModule(schedulerActor: ActorRef)(implicit actorSystem: ActorSystem)
    extends SchedulerAPI[QuckooIO] {
  import actorSystem.dispatcher

  override def cancelPlan(
      planId: PlanId
  ): QuckooIO[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]] = {
    val action = IO.fromFuture(Eval.later {
      implicit val timeout = Timeout(5 seconds)
      (schedulerActor ? CancelExecutionPlan(planId)).map {
        case msg: ExecutionPlanNotFound  => msg.asLeft[ExecutionPlanCancelled]
        case msg: ExecutionPlanCancelled => msg.asRight[ExecutionPlanNotFound]
      }
    })

    LiftIO[QuckooIO].liftIO(action)
  }

  override def fetchPlan(planId: PlanId) = ???

  override def allPlans = ???

  override def submit(jobId: JobId, trigger: Trigger, timeout: Option[FiniteDuration]) = ???

  override def fetchTask(taskId: TaskId) = ???

  override def allTasks = ???
}
