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

import diode.data.{AsyncAction, Pot, PotState}
import io.quckoo._
import io.quckoo.client.QuckooClient
import io.quckoo.console.ConsoleRoute
import io.quckoo.fault.Fault
import io.quckoo.id.{JobId, PlanId, TaskId}
import io.quckoo.net.QuckooState

import scala.util.{Failure, Try}
import scalaz.ValidationNel

final case class Login(username: String, password: String, referral: Option[ConsoleRoute] = None)
final case class LoggedIn(client: QuckooClient, referral: Option[ConsoleRoute])

case object Logout
case object LoggedOut
case object LoginFailed

final case class NavigateTo(route: ConsoleRoute)

final case class ClusterStateLoaded(state: QuckooState)
case object StartClusterSubscription

case object LoadJobSpecs
final case class JobSpecsLoaded(value: Map[JobId, Pot[JobSpec]])

final case class RefreshJobSpecs(
    keys: Set[JobId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[JobId, Pot[JobSpec]]] = Failure(new AsyncAction.PendingException)
  ) extends AsyncAction[Map[JobId, Pot[JobSpec]], RefreshJobSpecs] {

  override def next(newState: PotState, newValue: Try[Map[JobId, Pot[JobSpec]]]): RefreshJobSpecs =
    copy(state = newState, result = newValue)

}

final case class RegisterJobResult(jobId: ValidationNel[Fault, JobId])

case object LoadExecutionPlans
final case class ExecutionPlansLoaded(plans: Map[PlanId, Pot[ExecutionPlan]])

final case class RefreshExecutionPlans(
    keys: Set[PlanId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[PlanId, Pot[ExecutionPlan]]] = Failure(new AsyncAction.PendingException)
  ) extends AsyncAction[Map[PlanId, Pot[ExecutionPlan]], RefreshExecutionPlans] {

  override def next(newState: PotState, newValue: Try[Map[PlanId, Pot[ExecutionPlan]]]): RefreshExecutionPlans =
    copy(state = newState, result = newValue)

}

// TODO this is dummy object
final case class TaskItem()
case object LoadTasks

final case class TasksLoaded(tasks: Map[TaskId, Pot[TaskItem]])

final case class RefreshTasks(
    keys: Set[TaskId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[TaskId, Pot[TaskItem]]] = Failure(new AsyncAction.PendingException)
  ) extends AsyncAction[Map[TaskId, Pot[TaskItem]], RefreshTasks] {

  override def next(newState: PotState, newValue: Try[Map[TaskId, Pot[TaskItem]]]): RefreshTasks =
    copy(state = newState, result = newValue)

}
