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

package io.quckoo.worker.executor

import java.util.concurrent.Callable

import akka.actor.{ActorRef, Props}

import io.quckoo.fault.ExceptionThrown
import io.quckoo.id.{ArtifactId, TaskId}
import io.quckoo.resolver.Resolver
import io.quckoo.worker.core.{TaskExecutor, WorkerContext}

import scala.util.{Failure, Success, Try}

/**
  * Created by alonsodomin on 16/02/2017.
  */
object JarTaskExecutor {

  def props(workerContext: WorkerContext, taskId: TaskId, artifactId: ArtifactId, jobClass: String): Props =
    Props(new JarTaskExecutor(workerContext, taskId, artifactId, jobClass))

}

class JarTaskExecutor private (
    workerContext: WorkerContext,
    taskId: TaskId,
    artifactId: ArtifactId,
    jobClass: String)
  extends TaskExecutor {

  import TaskExecutor._
  import workerContext._

  def receive: Receive = ready

  private[this] def ready: Receive = {
    case Run =>
      log.info("Starting execution of task '{}' using class '{}' from artifact {}.",
        taskId, jobClass, artifactId
      )
      resolver ! Resolver.Download(artifactId)
      context.become(running(replyTo = sender()))
  }

  private[this] def running(replyTo: ActorRef): Receive = {
    case Resolver.ArtifactResolved(artifact) =>
      log.debug("Received resolved artifact for id: {}", artifactId)
      val result = for {
        job    <- artifact.newJob(jobClass, Map.empty)
        invoke <- runJob(job)
      } yield invoke

      val response = result match {
        case Success(value) => Completed(value)
        case Failure(ex)    => Failed(ExceptionThrown.from(ex))
      }
      complete(replyTo, response)

    case Resolver.ResolutionFailed(_, cause) =>
      val response = TaskExecutor.Failed(cause)
      complete(replyTo, response)
  }

  private[this] def completed(response: Response): Receive = {
    case _ => sender ! response
  }

  private def complete(replyTo: ActorRef, response: Response) = {
    log.debug("Completing execution of task '{}' with re")
    replyTo ! response
    context.become(completed(response))
  }

  private def runJob(callable: Callable[_]): Try[Any] = {
    log.debug("Running class '{}'...", jobClass)
    Try(callable.call())
  }

}
