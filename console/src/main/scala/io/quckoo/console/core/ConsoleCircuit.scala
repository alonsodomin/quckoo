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

import diode._
import diode.data.{AsyncAction, PotMap}
import diode.react.ReactConnector

import io.quckoo.auth.Passport
import io.quckoo.client.http.HttpQuckooClient
import io.quckoo.client.http.dom._
import io.quckoo.console.components.Notification
import io.quckoo.id.{JobId, PlanId, TaskId}
import io.quckoo.net.QuckooState
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker._
import io.quckoo.{ExecutionPlan, JobSpec, TaskExecution}

import org.threeten.bp.Clock

import slogging.LazyLogging

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalaz.{-\/, \/-}

/**
  * Created by alonsodomin on 20/02/2016.
  */
object ConsoleCircuit
    extends Circuit[ConsoleScope] with ReactConnector[ConsoleScope] with ConsoleOps
    with ConsoleSubscriptions with LazyLogging {

  object Implicits {
    implicit val consoleClock = Clock.systemDefaultZone
  }

  private[this] implicit val client: HttpQuckooClient = HttpDOMQuckooClient

  protected def initialModel: ConsoleScope = ConsoleScope.initial

  override protected def actionHandler = composeHandlers(
    loginHandler,
    clusterStateHandler,
    jobSpecMapHandler,
    registryHandler,
    scheduleHandler,
    executionPlanMapHandler,
    taskHandler,
    notificationHandler
  )

  def zoomIntoPassport: ModelRW[ConsoleScope, Option[Passport]] =
    zoomRW(_.passport) { (model, pass) =>
      model.copy(passport = pass)
    }

  def zoomIntoClusterState: ModelRW[ConsoleScope, QuckooState] =
    zoomRW(_.clusterState) { (model, value) =>
      model.copy(clusterState = value)
    }

  def zoomIntoUserScope: ModelRW[ConsoleScope, UserScope] =
    zoomRW(_.userScope) { (model, value) =>
      model.copy(userScope = value)
    }

  def zoomIntoExecutionPlans: ModelRW[ConsoleScope, PotMap[PlanId, ExecutionPlan]] =
    zoomIntoUserScope.zoomRW(_.executionPlans) { (model, plans) =>
      model.copy(executionPlans = plans)
    }

  def zoomIntoJobSpecs: ModelRW[ConsoleScope, PotMap[JobId, JobSpec]] =
    zoomIntoUserScope.zoomRW(_.jobSpecs) { (model, specs) =>
      model.copy(jobSpecs = specs)
    }

  def zoomIntoExecutions: ModelRW[ConsoleScope, PotMap[TaskId, TaskExecution]] =
    zoomIntoUserScope.zoomRW(_.executions) { (model, tasks) =>
      model.copy(executions = tasks)
    }

  val notificationHandler: HandlerFunction = (model, action) =>
    action match {
      case Growl(notification) =>
        notification.growl()
        None
  }

  val loginHandler = new ActionHandler(zoomIntoPassport) {

    override def handle = {
      case Login(username, password, referral) =>
        implicit val timeout = DefaultTimeout
        effectOnly(
          Effect(
            client.authenticate(username, password).map(pass => LoggedIn(pass, referral)).recover {
              case _ => LoginFailed
            }
          ))

      case Logout =>
        implicit val timeout = DefaultTimeout
        value.map { implicit passport =>
          effectOnly(Effect(client.signOut.map(_ => LoggedOut)))
        } getOrElse noChange
    }
  }

  val clusterStateHandler = new ActionHandler(zoomIntoClusterState) with AuthHandler[QuckooState] {

    override def handle = {
      case GetClusterStatus =>
        withAuth { implicit passport =>
          effectOnly(Effect(refreshClusterStatus))
        }

      case ClusterStateLoaded(state) =>
        updated(state, StartClusterSubscription)

      case StartClusterSubscription =>
        withAuth { implicit passport =>
          subscribeClusterState
          noChange
        }

      case evt: MasterEvent =>
        val notification = evt match {
          case MasterJoined(_, location) =>
            Notification.success(s"Master node joined from: $location")

          case MasterUnreachable(nodeId) =>
            Notification.warning(s"Master node $nodeId has become unreachable")

          case MasterReachable(nodeId) =>
            Notification.info(s"Master node $nodeId has re-joined the cluster")

          case MasterRemoved(nodeId) =>
            Notification.danger(s"Master node $nodeId has left the cluster.")
        }
        updated(value.updated(evt), Growl(notification))

      case evt: WorkerEvent =>
        val notification = evt match {
          case WorkerJoined(_, location) =>
            Notification.success(s"Worker node joined from: $location")

          case WorkerLost(nodeId) =>
            Notification.warning(s"Worker $nodeId lost communication with the cluster")

          case WorkerRemoved(nodeId) =>
            Notification.danger(s"Worker $nodeId has left the cluster")
        }
        updated(value.updated(evt), Growl(notification))

      case evt: TaskQueueUpdated =>
        updated(value.copy(metrics = value.metrics.updated(evt)))
    }

  }

  val registryHandler = new ActionHandler(zoomIntoUserScope) with AuthHandler[UserScope] {

    override def handle = {
      case RegisterJob(spec) =>
        withAuth { implicit passport =>
          effectOnly(Effect(registerJob(spec)))
        }

      case RegisterJobResult(validated) =>
        validated.disjunction match {
          case \/-(id) =>
            val notification = Notification.info(s"Job registered with id $id")
            val effects = Effects.set(
              Growl(notification),
              RefreshJobSpecs(Set(id))
            )
            effectOnly(effects)

          case -\/(errors) =>
            val effects = errors.map { err =>
              Notification.danger(err)
            }.map(n => Effect.action(Growl(n)))

            effectOnly(Effects.seq(effects))
        }

      case EnableJob(jobId) =>
        withAuth { implicit passport =>
          effectOnly(Effect(enableJob(jobId)))
        }

      case DisableJob(jobId) =>
        withAuth { implicit passport =>
          effectOnly(Effect(disableJob(jobId)))
        }
    }

  }

  val scheduleHandler = new ActionHandler(zoomIntoUserScope) with AuthHandler[UserScope] {

    override def handle = {
      case msg: ScheduleJob =>
        withAuth { implicit passport =>
          effectOnly(Effect(scheduleJob(msg)))
        }

      case CancelExecutionPlan(planId) =>
        withAuth { implicit passport =>
          effectOnly(Effect(cancelPlan(planId)))
        }

      case ExecutionPlanStarted(jobId, planId, _) =>
        val effect = Effects.set(
          Growl(Notification.success(s"Started execution plan for job. planId=$planId")),
          RefreshExecutionPlans(Set(planId))
        )
        effectOnly(effect)

      case ExecutionPlanFinished(jobId, planId, _) =>
        val effect = Effects.set(
          Growl(Notification.info(s"Execution plan $planId has finished")),
          RefreshExecutionPlans(Set(planId))
        )
        effectOnly(effect)

      case ExecutionPlanCancelled(_, planId, _) =>
        val effects = Effects.set(
          Growl(Notification.danger(s"Execution plan $planId has been cancelled")),
          RefreshExecutionPlans(Set(planId))
        )
        effectOnly(effects)

      case TaskScheduled(_, _, task, _) =>
        val effects = Effects.set(
          Growl(Notification.info(s"Task ${task.id} has been scheduled.")),
          RefreshExecutions(Set(task.id))
        )
        effectOnly(effects)

      case TaskTriggered(_, _, taskId, _) =>
        val effects = Effects.set(
          Growl(Notification.info(s"Task $taskId has been triggered.")),
          RefreshExecutions(Set(taskId))
        )
        effectOnly(effects)

      case TaskCompleted(_, _, taskId, _, _) =>
        val effects = Effects.set(
          Growl(Notification.info(s"Task $taskId has completed.")),
          RefreshExecutions(Set(taskId))
        )
        effectOnly(effects)
    }

  }

  val jobSpecMapHandler = new ActionHandler(zoomIntoJobSpecs)
  with AuthHandler[PotMap[JobId, JobSpec]] {

    override protected def handle = {
      case LoadJobSpecs =>
        withAuth { implicit passport =>
          effectOnly(Effect(loadJobSpecs().map(JobSpecsLoaded)))
        }

      case JobSpecsLoaded(specs) if specs.nonEmpty =>
        updated(PotMap(JobSpecFetcher, specs))

      case JobAccepted(jobId, spec) =>
        // TODO re-enable following code once registerJob command is fully async
        //val growl = Growl(Notification.info(s"Job accepted: $jobId"))
        //updated(value + (jobId -> Ready(spec)), growl)
        noChange

      case JobEnabled(jobId) =>
        effectOnly(
          Effects.set(
            Growl(Notification.info(s"Job enabled: $jobId")),
            RefreshJobSpecs(Set(jobId))
          ))

      case JobDisabled(jobId) =>
        effectOnly(
          Effects.set(
            Growl(Notification.info(s"Job disabled: $jobId")),
            RefreshJobSpecs(Set(jobId))
          ))

      case action: RefreshJobSpecs =>
        withAuth { implicit passport =>
          val updateEffect = action.effect(loadJobSpecs(action.keys))(identity)
          action.handleWith(this, updateEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

  val executionPlanMapHandler = new ActionHandler(zoomIntoExecutionPlans)
  with AuthHandler[PotMap[PlanId, ExecutionPlan]] {

    override protected def handle = {
      case LoadExecutionPlans =>
        withAuth { implicit passport =>
          effectOnly(Effect(loadPlans().map(ExecutionPlansLoaded)))
        }

      case ExecutionPlansLoaded(plans) if plans.nonEmpty =>
        updated(PotMap(ExecutionPlanFetcher, plans))

      case action: RefreshExecutionPlans =>
        withAuth { implicit passport =>
          val refreshEffect = action.effect(loadPlans(action.keys))(identity)
          action.handleWith(this, refreshEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

  val taskHandler = new ActionHandler(zoomIntoExecutions)
  with AuthHandler[PotMap[TaskId, TaskExecution]] {

    override protected def handle = {
      case LoadExecutions =>
        withAuth { implicit passport =>
          effectOnly(Effect(loadTasks().map(ExecutionsLoaded)))
        }

      case ExecutionsLoaded(tasks) if tasks.nonEmpty =>
        updated(PotMap(ExecutionFetcher, tasks))

      case action: RefreshExecutions =>
        withAuth { implicit passport =>
          val refreshEffect = action.effect(loadTasks(action.keys))(identity)
          action.handleWith(this, refreshEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

}
