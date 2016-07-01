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
import io.quckoo.protocol.cluster.{GetClusterStatus, MasterEvent}
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
    taskHandler
  )

  def zoomIntoNotification: ModelRW[ConsoleScope, Option[Notification]] =
    zoomRW(_.notification)((model, notif) => model.copy(notification = notif))

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
        updated(state, Effect.action(StartClusterSubscription))

      case StartClusterSubscription =>
        withClient { implicit client =>
          subscribeClusterState
          noChange
        }

      case evt: MasterEvent =>
        updated(value.updated(evt))

      case evt: WorkerEvent =>
        updated(value.updated(evt))

      case evt: TaskQueueUpdated =>
        updated(value.copy(metrics = value.metrics.updated(evt)))
    }

  }

  val registryHandler = new ActionHandler(zoomIntoNotification)
      with ConnectedHandler[Option[Notification]] {

    override def handle = {
      case RegisterJob(spec) =>
        withClient { client =>
          updated(None, Effect(client.registerJob(spec).map(RegisterJobResult)))
        }

      case RegisterJobResult(validated) =>
        validated.disjunction match {
          case \/-(id) =>
            updated(
              Some(Notification.success(s"Job registered with id $id")),
              Effect.action(RefreshJobSpecs(Set(id)))
            )

          case -\/(errors) =>
            updated(Some(Notification.danger(errors)))
        }

      case EnableJob(jobId) =>
        withClient { client =>
          updated(None, Effect(client.enableJob(jobId)))
        }

      case DisableJob(jobId) =>
        withClient { client =>
          updated(None, Effect(client.disableJob(jobId)))
        }
    }

  }

  val scheduleHandler = new ActionHandler(zoomIntoNotification)
      with ConnectedHandler[Option[Notification]] {

    override def handle = {
      case msg: ScheduleJob =>
        withClient { client =>
          updated(None, Effect(client.schedule(msg).map(_.fold(identity, identity))))
        }

      case JobNotFound(jobId) =>
        updated(Some(Notification.danger(s"Job not found $jobId")))

      case ExecutionPlanStarted(jobId, planId) =>
        val effect = Effect.action(RefreshExecutionPlans(Set(planId)))
        updated(Some(Notification.success(s"Started execution plan for job. planId=$planId")), effect)
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
        effectOnly(Effect.action(RefreshJobSpecs(Set(jobId))))

      case JobDisabled(jobId) =>
        effectOnly(Effect.action(RefreshJobSpecs(Set(jobId))))

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
