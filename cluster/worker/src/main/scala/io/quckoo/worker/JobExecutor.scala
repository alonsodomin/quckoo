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

package io.quckoo.worker

import akka.actor.{Actor, ActorLogging, Props}
import io.quckoo.Task
import io.quckoo.fault.{ExceptionThrown, Fault}
import io.quckoo.resolver.{Artifact, Resolve}

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  final case class Execute(task: Task, artifact: Artifact)
  final case class Failed(error: Fault)
  final case class Completed(result: Any)

  def props: Props = Props(classOf[JobExecutor])
}

class JobExecutor extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case Execute(task, artifact) =>
      val result = for {
        job    <- artifact.newJob(task.jobClass, task.params)
        invoke <- Try(job.call())
      } yield invoke

      val response = result match {
        case Success(value) => Completed(value)
        case Failure(ex)    => Failed(ExceptionThrown(ex))
      }

      sender() ! response
  }

}
