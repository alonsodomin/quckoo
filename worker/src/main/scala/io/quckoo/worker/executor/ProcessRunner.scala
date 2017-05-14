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

import java.io._

import cats.effect.IO
import cats.implicits._

import resource._

import kamon.trace.Tracer

import scala.concurrent.ExecutionContext

import slogging._

object ProcessRunner {

  final case class Result(exitCode: Int, stdOut: String, stdErr: String)

}

class ProcessRunner(command: String, args: String*) extends StrictLogging {
  import ProcessRunner._

  def run(implicit executionContext: ExecutionContext): IO[Result] = {
    def readStream(stream: InputStream): IO[String] = IO {
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

    def startProcess(builder: ProcessBuilder): IO[Process] = IO {
      logger.info("Executing command: {}", command)
      builder.start()
    }

    def captureResult(proc: Process): IO[Result] = {
      def exitCode = IO { proc.waitFor() } shift
      def stdOut = readStream(proc.getInputStream).shift
      def stdErr = readStream(proc.getErrorStream).shift

      (exitCode |@| stdOut |@| stdErr).map(Result)
    }

    val commandLine = command +: args
    val procBuilder = new ProcessBuilder(commandLine: _*)

    Tracer.withNewContext(s"process-$command") {
      startProcess(procBuilder) >>= captureResult
    }
  }

}
