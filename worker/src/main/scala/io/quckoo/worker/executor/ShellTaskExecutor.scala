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

import io.quckoo.{ExceptionThrown, ShellScriptPackage, TaskExitCodeFault, TaskId}
import io.quckoo.worker.core.{TaskExecutor, WorkerContext}

import scala.concurrent.Future

import scalaz._
import Scalaz._

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

      withRunner {
        _.run.map { result =>
          if (result.exitCode == 0) {
            Completed(result.stdOut)
          } else {
            Failed(TaskExitCodeFault(result.exitCode))
          }
        } recover {
          case ex => Failed(ExceptionThrown.from(ex))
        }
      } pipeTo sender()
  }

  private [this] def withRunner[R](f: ProcessRunner => Future[R]): Future[R] = {
    import context.dispatcher

    def generateScript = {
      Future {
        val tempFile = File.newTemporaryFile()
        tempFile.append(shellPackage.content)
        tempFile.addPermission(PosixFilePermission.OWNER_EXECUTE)
        tempFile
      }
    }

    def runIt(file: File): Future[R] = {
      def runScript: Future[R] = {
        log.debug("Running file: {}", file)
        val runner = new ProcessRunner(file.path.toString)
        f(runner)
      }

      def deleteScript: Future[Unit] = Future {
        log.debug("Deleting script")
        file.delete(true)
        ()
      }

      runScript.flatMap(r => deleteScript.map(_ => r))
    }

    generateScript >>= runIt
  }

}
