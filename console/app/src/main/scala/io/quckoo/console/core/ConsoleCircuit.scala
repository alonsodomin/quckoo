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

import io.quckoo.client.QuckooClient
import io.quckoo.client.ajax.AjaxQuckooClientFactory
import io.quckoo.console.components.Notification
import io.quckoo.id.{JobId, PlanId, TaskId}
import io.quckoo.net.QuckooState
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.worker._
import io.quckoo.{ExecutionPlan, JobSpec}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalaz.{-\/, \/-}

/**
  * Created by alonsodomin on 20/02/2016.
  */
object ConsoleCircuit extends Circuit[ConsoleScope] with ReactConnector[ConsoleScope]
    with ConsoleOps with ConsoleSubscriptions {

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

  def zoomIntoClient: ModelRW[ConsoleScope, Option[QuckooClient]] =
    zoomRW(_.client) { (model, c) => model.copy(client = c) }

  def zoomIntoClusterState: ModelRW[ConsoleScope, QuckooState] =
    zoomRW(_.clusterState) { (model, value) => model.copy(clusterState = value) }

  def zoomIntoUserScope: ModelRW[ConsoleScope, UserScope] =
    zoomRW(_.userScope) { (model, value) => model.copy(userScope = value) }

  def zoomIntoExecutionPlans: ModelRW[ConsoleScope, PotMap[PlanId, ExecutionPlan]] =
    zoomIntoUserScope.zoomRW(_.executionPlans) { (model, plans) => model.copy(executionPlans = plans) }

  def zoomIntoJobSpecs: ModelRW[ConsoleScope, PotMap[JobId, JobSpec]] =
    zoomIntoUserScope.zoomRW(_.jobSpecs) { (model, specs) => model.copy(jobSpecs = specs) }

  def zoomIntoTasks: ModelRW[ConsoleScope, PotMap[TaskId, TaskDetails]] =
    zoomIntoUserScope.zoomRW(_.tasks) { (model, tasks) => model.copy(tasks = tasks) }

  val notificationHandler: HandlerFunction = (model, action) => action match {
    case Growl(notification) =>
      notification.growl()
      None
  }

  val loginHandler = new ActionHandler(zoomIntoClient) {

    override def handle = {
      case Login(username, password, referral) =>
        effectOnly(Effect(
          AjaxQuckooClientFactory.connect(username, password).
            map(client => LoggedIn(client, referral)).
            recover { case _ => LoginFailed }
        ))

      case Logout =>
        value.map { client =>
          effectOnly(Effect(client.close().map(_ => LoggedOut)))
        } getOrElse noChange
    }
  }

  val clusterStateHandler = new ActionHandler(zoomIntoClusterState)
      with ConnectedHandler[QuckooState] {

    override def handle = {
      case GetClusterStatus =>
        withClient { implicit client =>
          effectOnly(Effect(refreshClusterStatus))
        }

      case ClusterStateLoaded(state) =>
        updated(state, StartClusterSubscription)

      case StartClusterSubscription =>
        withClient { implicit client =>
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

          case WorkerRemoved(nodeId) =>
            Notification.danger(s"Worker $nodeId has left the cluster")
        }
        updated(value.updated(evt), Growl(notification))

      case evt: TaskQueueUpdated =>
        updated(value.copy(metrics = value.metrics.updated(evt)))
    }

  }

  val registryHandler = new ActionHandler(zoomIntoUserScope)
      with ConnectedHandler[UserScope] {

    override def handle = {
      case RegisterJob(spec) =>
        withClient { client =>
          effectOnly(Effect(client.registerJob(spec).map(RegisterJobResult)))
        }

      case RegisterJobResult(validated) =>
        validated.disjunction match {
          case \/-(id) =>
            val notification = Notification.info(s"Job registered with id $id")
            val effects = Effects.seq(
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
        withClient { client =>
          effectOnly(Effect(client.enableJob(jobId)))
        }

      case DisableJob(jobId) =>
        withClient { client =>
          effectOnly(Effect(client.disableJob(jobId)))
        }
    }

  }

  val scheduleHandler = new ActionHandler(zoomIntoUserScope)
      with ConnectedHandler[UserScope] {

    override def handle = {
      case msg: ScheduleJob =>
        withClient { client =>
          effectOnly(Effect(client.schedule(msg).map(_.fold(identity, identity))))
        }

      case CancelExecutionPlan(planId) =>
        withClient { client =>
          effectOnly(Effect(client.cancelPlan(planId).map(_ => ExecutionPlanCancelled(planId))))
        }

      case JobNotFound(jobId) =>
        effectOnly(Growl(
          Notification.danger(s"Job not found $jobId")
        ))

      case ExecutionPlanStarted(jobId, planId) =>
        val effect = Effects.set(
          Growl(Notification.success(s"Started execution plan for job. planId=$planId")),
          RefreshExecutionPlans(Set(planId))
        )
        effectOnly(effect)

      case ExecutionPlanCancelled(planId) =>
        effectOnly(Growl(
          Notification.success(s"Execution plan $planId has been cancelled")
        ))
    }

  }

  val jobSpecMapHandler = new ActionHandler(zoomIntoJobSpecs)
      with ConnectedHandler[PotMap[JobId, JobSpec]] {

    override protected def handle = {
      case LoadJobSpecs =>
        withClient { implicit client =>
          effectOnly(Effect(loadJobSpecs().map(JobSpecsLoaded)))
        }

      case JobSpecsLoaded(specs) if specs.nonEmpty =>
        updated(PotMap(JobSpecFetcher, specs))

      case JobEnabled(jobId) =>
        effectOnly(Effects.set(
          Growl(Notification.info(s"Job enabled: $jobId")),
          RefreshJobSpecs(Set(jobId))
        ))

      case JobDisabled(jobId) =>
        effectOnly(Effects.set(
          Growl(Notification.info(s"Job disabled: $jobId")),
          RefreshJobSpecs(Set(jobId))
        ))

      case action: RefreshJobSpecs =>
        withClient { implicit client =>
          val updateEffect = action.effect(loadJobSpecs(action.keys))(identity)
          action.handleWith(this, updateEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

  val executionPlanMapHandler = new ActionHandler(zoomIntoExecutionPlans)
      with ConnectedHandler[PotMap[PlanId, ExecutionPlan]] {

    override protected def handle = {
      case LoadExecutionPlans =>
        withClient { implicit client =>
          effectOnly(Effect(loadPlans().map(ExecutionPlansLoaded)))
        }

      case ExecutionPlansLoaded(plans) if plans.nonEmpty =>
        updated(PotMap(ExecutionPlanFetcher, plans))

      case action: RefreshExecutionPlans =>
        withClient { implicit client =>
          val refreshEffect = action.effect(loadPlans(action.keys))(identity)
          action.handleWith(this, refreshEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

  val taskHandler = new ActionHandler(zoomIntoTasks)
      with ConnectedHandler[PotMap[TaskId, TaskDetails]] {

    override protected def handle = {
      case LoadTasks =>
        withClient { implicit client =>
          effectOnly(Effect(loadTasks().map(TasksLoaded)))
        }

      case TasksLoaded(tasks) if tasks.nonEmpty =>
        updated(PotMap(TaskFetcher, tasks))

      case action: RefreshTasks =>
        withClient { implicit client =>
          val refreshEffect = action.effect(loadTasks(action.keys))(identity)
          action.handleWith(this, refreshEffect)(AsyncAction.mapHandler(action.keys))
        }
    }

  }

}
