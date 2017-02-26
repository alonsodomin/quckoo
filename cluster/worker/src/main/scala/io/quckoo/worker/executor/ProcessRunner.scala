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

import java.io._

import resource._

import scala.concurrent._

import scalaz._
import Scalaz._

import slogging._

object ProcessRunner {

  final case class Result(exitCode: Int, stdOut: String, stdErr: String)

}

class ProcessRunner(command: String, args: String*) extends StrictLogging {
  import ProcessRunner._

  def run(implicit executionContext: ExecutionContext): Future[Result] = {
    logger.info("Executing command: {}", command)

    def readStream(stream: InputStream): Future[String] = Future {
      blocking {
        val managedOutput = for {
          buffer <- managed(new StringWriter())
          writer <- managed(new PrintWriter(buffer))
          input  <- managed(new BufferedReader(new InputStreamReader(stream)))
        } yield {
          def read(): Unit = input.readLine() match {
            case null => ()
            case line =>
              writer.println(line)
              read()
          }

          read()
          buffer.toString
        }

        managedOutput.acquireAndGet(identity)
      }
    }

    def startProcess(builder: ProcessBuilder): Future[Process] =
      Future(builder.start())

    def captureOutput(proc: Process): Future[(String, String)] = {
      val stdOut = readStream(proc.getInputStream())
      val stdErr = readStream(proc.getErrorStream())
      (stdOut |@| stdErr)(_ -> _)
    }

    def waitForCompletion(proc: Process): Future[Int] =
      Future { blocking { proc.waitFor() } }

    val commandLine = command +: args
    val procBuilder = new ProcessBuilder(commandLine: _*)

    for {
      proc       <- startProcess(procBuilder)
      exitCode   <- waitForCompletion(proc)
      (out, err) <- captureOutput(proc)
    } yield Result(exitCode, out, err)
  }

}
