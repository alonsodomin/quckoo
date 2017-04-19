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

import cats.data.{NonEmptyList, ValidatedNel}

import diode.data.{AsyncAction, Pot, PotState}

import io.quckoo._
import io.quckoo.auth.Passport
import io.quckoo.console.ConsoleRoute
import io.quckoo.console.components.Notification
import io.quckoo.net.QuckooState
import io.quckoo.protocol.{Command, Event}

import scala.util.{Failure, Try}

final case class Failed(fault: NonEmptyList[QuckooError]) extends Event

final case class Login(username: String, password: String, referral: Option[ConsoleRoute] = None)
    extends Command
final case class LoggedIn(passport: Passport, referral: Option[ConsoleRoute]) extends Event

case object Logout      extends Command
case object LoggedOut   extends Event
case object LoginFailed extends Event

final case class NavigateTo(route: ConsoleRoute)   extends Command
final case class Growl(notification: Notification) extends Command

final case class ClusterStateLoaded(state: QuckooState) extends Event
case object StartClusterSubscription                    extends Command

case object LoadJobSpecs                                         extends Command
final case class JobSpecsLoaded(value: Map[JobId, Pot[JobSpec]]) extends Event

final case class RefreshJobSpecs(
    keys: Set[JobId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[JobId, Pot[JobSpec]]] = Failure(new AsyncAction.PendingException)
) extends AsyncAction[Map[JobId, Pot[JobSpec]], RefreshJobSpecs] {

  override def next(newState: PotState, newValue: Try[Map[JobId, Pot[JobSpec]]]): RefreshJobSpecs =
    copy(state = newState, result = newValue)

}

final case class RegisterJobResult(jobId: ValidatedNel[QuckooError, JobId]) extends Event

case object LoadExecutionPlans                                                extends Command
final case class ExecutionPlansLoaded(plans: Map[PlanId, Pot[ExecutionPlan]]) extends Event

final case class RefreshExecutionPlans(
    keys: Set[PlanId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[PlanId, Pot[ExecutionPlan]]] = Failure(new AsyncAction.PendingException)
) extends AsyncAction[Map[PlanId, Pot[ExecutionPlan]], RefreshExecutionPlans] {

  override def next(newState: PotState,
                    newValue: Try[Map[PlanId, Pot[ExecutionPlan]]]): RefreshExecutionPlans =
    copy(state = newState, result = newValue)

}

case object LoadExecutions extends Command

final case class ExecutionsLoaded(tasks: Map[TaskId, Pot[TaskExecution]]) extends Event

final case class RefreshExecutions(
    keys: Set[TaskId],
    state: PotState = PotState.PotEmpty,
    result: Try[Map[TaskId, Pot[TaskExecution]]] = Failure(new AsyncAction.PendingException)
) extends AsyncAction[Map[TaskId, Pot[TaskExecution]], RefreshExecutions] {

  override def next(newState: PotState,
                    newValue: Try[Map[TaskId, Pot[TaskExecution]]]): RefreshExecutions =
    copy(state = newState, result = newValue)

}
