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

import io.quckoo.kamon._

import scala.concurrent.ExecutionContext

import slogging._

object ProcessRunner {

  final case class Result(exitCode: Int, stdOut: String, stdErr: String)

  private def resource[A <: Closeable, B](acquire: IO[A])(use: A => IO[B]): IO[B] = {
    acquire.flatMap { resource =>
      use(resource).attempt.flatMap { result =>
        val exit = result match {
          case Right(value) => IO.pure(value)
          case Left(error)  => IO.raiseError(error)
        }
        IO(resource.close()).attempt.void *> exit
      }
    }
  }

}

class ProcessRunner(command: String, args: String*) extends StrictLogging {
  import ProcessRunner._

  def run(implicit executionContext: ExecutionContext): IO[Result] = {
    def readStream(stream: InputStream): IO[String] = {
      def bufferInput(buffer: StringWriter): IO[String] = {
        val acquirePW = IO(new PrintWriter(buffer))
        val acquireBR = IO(new BufferedReader(new InputStreamReader(stream)))

        resource(acquirePW) { writer =>
          resource(acquireBR)(input => readInput(input, writer))
        } *> IO(buffer.toString)
      }

      def readInput(input: BufferedReader, writer: PrintWriter): IO[Unit] = {
        def read(): IO[Unit] = IO(input.readLine()).flatMap {
          case null => IO.unit
          case line => IO(writer.println(line)) *> IO.suspend(read())
        }

        read()
      }

      resource(IO(new StringWriter()))(bufferInput)
    }

    def startProcess(builder: ProcessBuilder): IO[Process] = IO {
      logger.info("Executing command: {}", command)
      builder.start()
    }

    def captureResult(proc: Process): IO[Result] = {
      def exitCode = IO.shift *> IO { proc.waitFor() }
      def stdOut   = IO.shift *> readStream(proc.getInputStream)
      def stdErr   = IO.shift *> readStream(proc.getErrorStream)

      (exitCode, stdOut, stdErr).mapN(Result)
    }

    val commandLine = command +: args
    val procBuilder = new ProcessBuilder(commandLine: _*)

    (startProcess(procBuilder) >>= captureResult).trace(s"process-$command")
  }

}
