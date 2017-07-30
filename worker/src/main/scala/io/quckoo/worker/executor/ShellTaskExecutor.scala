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

package io.quckoo.worker.executor

import java.nio.file.attribute.PosixFilePermission

import akka.actor.Props
import akka.pattern._

import better.files._

import cats.effect.IO
import cats.implicits._

import io.quckoo.{
  ExceptionThrown,
  ShellScriptPackage,
  TaskExitCodeFault,
  TaskId
}
import io.quckoo.worker.core.{TaskExecutor, WorkerContext}

object ShellTaskExecutor {

  def props(workerContext: WorkerContext,
            taskId: TaskId,
            shellPackage: ShellScriptPackage): Props =
    Props(new ShellTaskExecutor(workerContext, taskId: TaskId, shellPackage))

}

class ShellTaskExecutor private (
    workerContext: WorkerContext,
    taskId: TaskId,
    shellPackage: ShellScriptPackage
) extends TaskExecutor {

  import TaskExecutor._

  def receive: Receive = {
    case TaskExecutor.Run =>
      import context.dispatcher

      val program = withRunner {
        _.run.map { result =>
          if (result.exitCode == 0) {
            Completed(result.stdOut)
          } else {
            Failed(TaskExitCodeFault(result.exitCode))
          }
        } recover {
          case ex => Failed(ExceptionThrown.from(ex))
        }
      }

      program.unsafeToFuture() pipeTo sender()
  }

  private[this] def withRunner[R](f: ProcessRunner => IO[R]): IO[R] = {
    def generateScript = IO {
      val tempFile = File.newTemporaryFile()
      tempFile.append(shellPackage.content)
      tempFile.addPermission(PosixFilePermission.OWNER_EXECUTE)
      tempFile
    }

    def runIt(file: File): IO[R] = {
      def runScript: IO[R] = {
        log.debug("Running file: {}", file)
        val runner = new ProcessRunner(file.path.toString)
        f(runner)
      }

      def deleteScript: IO[Unit] = IO {
        log.debug("Deleting script")
        file.delete(true)
        ()
      }

      runScript.attempt.flatMap(e =>
        deleteScript.flatMap(_ => e.fold(IO.raiseError, IO.pure)))
    }

    generateScript >>= runIt
  }

}
