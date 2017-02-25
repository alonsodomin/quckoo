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

import java.nio.file.attribute.PosixFilePermission

import akka.actor.Props
import akka.pattern._

import better.files._

import io.quckoo.ShellScriptPackage
import io.quckoo.id.TaskId
import io.quckoo.fault.{ExceptionThrown, TaskExecutionFault}
import io.quckoo.worker.core.{TaskExecutor, WorkerContext}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Success, Failure}

object ShellTaskExecutor {

  def props(workerContext: WorkerContext, taskId: TaskId, shellPackage: ShellScriptPackage): Props =
    Props(new ShellTaskExecutor(workerContext, taskId: TaskId, shellPackage))

}

class ShellTaskExecutor private (
    workerContext: WorkerContext, taskId: TaskId, shellPackage: ShellScriptPackage
  ) extends TaskExecutor {

  import TaskExecutor._

  def receive: Receive = {
    case TaskExecutor.Run =>
      import context.dispatcher
      val scriptFile = generateScriptFile()
      val runner = new ShellProcessRunner(scriptFile.path.toString())

      runner.run.map { result =>
        if (result.exitCode == 0) {
          Completed(result.stdOut)
        } else {
          Failed(TaskExecutionFault(result.exitCode))
        }
      } recover {
        case ex => Failed(ExceptionThrown.from(ex))
      } pipeTo sender()
  }

  private [this] def generateScriptFile() = {
    val scriptFile = File.newTemporaryFile()
    scriptFile.append(shellPackage.content)
    scriptFile.addPermission(PosixFilePermission.OWNER_EXECUTE)
    //scriptFile.deleteOnExit()
    scriptFile
  }

}
