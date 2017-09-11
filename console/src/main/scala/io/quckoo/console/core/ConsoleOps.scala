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

import cats.Functor
import cats.data.{EitherT, NonEmptyList, ValidatedNel}
import cats.implicits._

import diode.data.{Pot, Ready, Unavailable}

import io.quckoo._
import io.quckoo.net.QuckooState
import io.quckoo.protocol.Event
import io.quckoo.protocol.scheduler.ScheduleJob

import slogging.LoggerHolder

/**
  * Created by alonsodomin on 25/03/2016.
  */
trait ConsoleOps { this: LoggerHolder =>

  def login(login: Login): ConsoleIO[Event] =
    ConsoleIO
      .remote(_.signIn(login.username, login.password))
      .map[Event](_ => LoggedIn(login.referral))
      .handleError(_ => LoginFailed)

  def logout(): ConsoleIO[Event] =
    ConsoleIO.remote(_.signOut()).map(_ => LoggedOut)

  def currentClusterState: ConsoleIO[QuckooState] =
    for {
      _            <- ConsoleIO.local(logger.debug("Refreshing cluster status..."))
      clusterState <- ConsoleIO.remote(_.currentState)
    } yield clusterState

  def registerJob(jobSpec: JobSpec): ConsoleIO[ValidatedNel[QuckooError, JobId]] =
    ConsoleIO.remote(_.registerJob(jobSpec))

  def enableJob(jobId: JobId): ConsoleIO[Event] =
    foldIntoEvent(ConsoleIO.remote(_.enableJob(jobId)))

  def disableJob(jobId: JobId): ConsoleIO[Event] =
    foldIntoEvent(ConsoleIO.remote(_.disableJob(jobId)))

  def loadJobSpec(jobId: JobId): ConsoleIO[(JobId, Pot[JobSpec])] =
    ConsoleIO.remote(_.fetchJob(jobId)).map {
      case Some(spec) => (jobId, Ready(spec))
      case None       => (jobId, Unavailable)
    }

  def loadJobSpecs(keys: Set[JobId] = Set.empty): ConsoleIO[Map[JobId, Pot[JobSpec]]] = {
    def loadAll: ConsoleIO[Map[JobId, Pot[JobSpec]]] =
      for {
        _      <- ConsoleIO.local(logger.debug("Loading all jobs from the server..."))
        result <- ConsoleIO.remote(_.allJobs).map(_.map { case (k, v) => (k, Ready(v)) }.toMap)
      } yield result

    def loadFromSet: ConsoleIO[Map[JobId, Pot[JobSpec]]] =
      for {
        _      <- ConsoleIO.local(logger.debug("Loading job specs for ids: {}", keys.mkString(", ")))
        result <- keys.toList.traverse(loadJobSpec).map(_.toMap)
      } yield result

    if (keys.isEmpty) loadAll
    else loadFromSet
  }

  def scheduleJob(details: ScheduleJob): ConsoleIO[Event] =
    foldIntoEvent(ConsoleIO.remote(_.submit(details.jobId, details.trigger, details.timeout)))

  def cancelPlan(planId: PlanId): ConsoleIO[Event] =
    foldIntoEvent(ConsoleIO.remote(_.cancelPlan(planId)))

  def loadPlans(ids: Set[PlanId] = Set.empty): ConsoleIO[Map[PlanId, Pot[ExecutionPlan]]] = {
    def loadAll: ConsoleIO[Map[PlanId, Pot[ExecutionPlan]]] =
      for {
        _      <- ConsoleIO.local(logger.debug("Loading all execution plans from the server"))
        result <- ConsoleIO.remote(_.allPlans.map(_.map { case (k, v) => (k, Ready(v)) }.toMap))
      } yield result

    def loadFromSet: ConsoleIO[Map[PlanId, Pot[ExecutionPlan]]] =
      for {
        _ <- ConsoleIO.local(
          logger.debug("Loading execution plans for ids: {}", ids.mkString(", "))
        )
        result <- ids.toList.traverse(loadPlan).map(_.toMap)
      } yield result

    if (ids.isEmpty) loadAll
    else loadFromSet
  }

  def loadPlan(id: PlanId): ConsoleIO[(PlanId, Pot[ExecutionPlan])] =
    ConsoleIO.remote(_.fetchPlan(id)).map {
      case Some(plan) => id -> Ready(plan)
      case None       => id -> Unavailable
    }

  def loadTasks(ids: Set[TaskId] = Set.empty): ConsoleIO[Map[TaskId, Pot[TaskExecution]]] =
    if (ids.isEmpty) {
      ConsoleIO.remote(_.allTasks).map(_.map { case (k, v) => (k, Ready(v)) }.toMap)
    } else {
      ids.toList.traverse(loadTask).map(_.toMap)
    }

  def loadTask(id: TaskId): ConsoleIO[(TaskId, Pot[TaskExecution])] =
    ConsoleIO.remote(_.fetchTask(id)).map {
      case Some(task) => id -> Ready(task)
      case None       => id -> Unavailable
    }

  private[this] def foldIntoEvent[F[_]: Functor, E <: QuckooError, A <: Event](
      f: => F[Either[E, A]]
  ): F[Event] =
    EitherT(f)
      .leftMap(fault => Failed(NonEmptyList.of[QuckooError](fault)))
      .fold(identity, identity)

}
