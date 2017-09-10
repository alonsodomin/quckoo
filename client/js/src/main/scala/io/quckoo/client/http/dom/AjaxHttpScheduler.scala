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

package io.quckoo.client.http.dom

import cats.Eval
import cats.effect.IO
import cats.implicits._

import io.circe.parser.decode
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.syntax._

import io.quckoo._
import io.quckoo.api2.Scheduler
import io.quckoo.client.ClientIO
import io.quckoo.client.http._
import io.quckoo.protocol.scheduler.{ExecutionPlanCancelled, ExecutionPlanStarted, ScheduleJob}
import io.quckoo.serialization.json._
import io.quckoo.util._

import org.scalajs.dom.ext.Ajax

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

trait AjaxHttpScheduler extends Scheduler[ClientIO] {

  override def cancelPlan(
      planId: PlanId
  ): ClientIO[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]] =
    ClientIO.auth { session =>
      val uri = s"$ExecutionPlansURI/$planId"
      val ajax = IO.fromFuture(Eval.later {
        Ajax.delete(uri, headers = Map(bearerToken(session.passport)))
      })

      ajax >>= handleResponse {
        case res if res.status == 200 =>
          attempt2IO(decode[ExecutionPlanCancelled](res.responseText))
            .map(_.asRight[ExecutionPlanNotFound])

        case res if res.status == 404 =>
          IO.pure(Left(ExecutionPlanNotFound(planId)))
      }
    }

  override def fetchPlan(planId: PlanId): ClientIO[Option[ExecutionPlan]] =
    ClientIO.auth { session =>
      val uri = s"$ExecutionPlansURI/$planId"
      val ajax = IO.fromFuture(Eval.later {
        Ajax.get(uri, headers = Map(bearerToken(session.passport)))
      })

      ajax >>= handleResponse {
        case res if res.status == 200 =>
          attempt2IO(decode[ExecutionPlan](res.responseText))
            .map(Some(_))

        case res if res.status == 404 =>
          IO.pure(None)
      }
    }

  override def fetchTask(taskId: TaskId): ClientIO[Option[TaskExecution]] =
    ClientIO.auth { session =>
      val uri = s"$TaskExecutionsURI/$taskId"
      val ajax = IO.fromFuture(Eval.later {
        Ajax.get(uri, headers = Map(bearerToken(session.passport)))
      })

      ajax >>= handleResponse {
        case res if res.status == 200 =>
          attempt2IO(decode[TaskExecution](res.responseText)).map(Some(_))

        case res if res.status == 404 =>
          IO.pure(None)
      }
    }

  override def submit(
      jobId: JobId,
      trigger: Trigger,
      timeout: Option[FiniteDuration]
  ): ClientIO[Either[InvalidJob, ExecutionPlanStarted]] =
    ClientIO.auth { session =>
      val payload = ScheduleJob(jobId, trigger, timeout).asJson.noSpaces
      val hdrs = Map(
        JsonContentTypeHeader,
        bearerToken(session.passport)
      )
      val ajax = IO.fromFuture(Eval.later {
        Ajax.put(ExecutionPlansURI, headers = hdrs, data = payload)
      })

      ajax >>= handleResponse {
        case res if res.status == 200 =>
          attempt2IO(decode[ExecutionPlanStarted](res.responseText))
            .map(_.asRight[InvalidJob])

        case res if res.status == 400 =>
          attempt2IO(decode[InvalidJob](res.responseText))
            .map(_.asLeft[ExecutionPlanStarted])
      }
    }

  override def allPlans = ???

  override def allTasks = ???
}
