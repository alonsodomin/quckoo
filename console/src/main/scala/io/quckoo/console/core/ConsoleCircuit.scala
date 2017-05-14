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

package io.quckoo.console.core

import java.time.Clock

import diode._
import diode.data.{AsyncAction, PotMap}
import diode.react.ReactConnector

import io.quckoo._
import io.quckoo.auth.Passport
import io.quckoo.client.http.HttpQuckooClient
import io.quckoo.client.http.dom._
import io.quckoo.console.components.Notification
import io.quckoo.console.registry.RegistryHandler
import io.quckoo.net.QuckooState
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker._

import slogging.LazyLogging

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

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
    new RegistryHandler(zoomIntoJobSpecs, this),
    scheduleHandler,
    executionPlanMapHandler,
    taskHandler,
    globalHandler
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

  val globalHandler: HandlerFunction = (model, action) =>
    action match {
      case Growl(notification) =>
        notification.growl()
        None

      case StartClusterSubscription =>
        if (!model.subscribed) {
          model.passport.map { implicit passport =>
            logger.debug("Opening console subscription channels...")
            openSubscriptionChannels
            ActionResult.ModelUpdate(model.copy(subscribed = true))
          }
        } else None
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
        updated(state)

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
        val effect = Effects.parallel(
          Growl(Notification.success(s"Started execution plan for job. planId=$planId")),
          RefreshExecutionPlans(Set(planId))
        )
        effectOnly(effect)

      case ExecutionPlanFinished(jobId, planId, _) =>
        effectOnly(RefreshExecutionPlans(Set(planId)))

      case ExecutionPlanCancelled(_, planId, _) =>
        val effects = Effects.parallel(
          Growl(Notification.success(s"Execution plan $planId has been cancelled")),
          RefreshExecutionPlans(Set(planId))
        )
        effectOnly(effects)

      case TaskScheduled(_, _, task, _) =>
        effectOnly(RefreshExecutions(Set(task.id)))

      case TaskTriggered(_, _, taskId, _) =>
        effectOnly(RefreshExecutions(Set(taskId)))

      case TaskCompleted(_, _, taskId, _, _) =>
        effectOnly(RefreshExecutions(Set(taskId)))
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
        logger.debug(s"Loaded ${plans.size} execution plans from the server.")
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
