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

package io.quckoo.shell

import cats.effect.Sync
import cats.implicits._

import io.quckoo.Info
import io.quckoo.shell.console.Console

/**
  * Created by alonsodomin on 03/06/2017.
  */
trait Shell[F[_]] {

  def console: Console[F]

}

class RunnableShell[F[_]](val console: Console[F], commands: Map[String, CommandParser], quitCmd: String = "quit")(implicit F: Sync[F]) extends Shell[F] {

  def commandNotFound(cmdName: String): F[Unit] =
    console.printLine(s"Command not found: $cmdName")

  def runInteractive: F[Unit] = {
    def welcome: F[Unit] = console.printLine(s"Quckoo v${Info.version}")

    def executeCmdLine(cmd: String, args: Seq[String]): F[Unit] = {
      if (cmd === quitCmd) Quit.run(this)
      else {
        commands.get(cmd).map { parser =>
          parser.parse(args) match {
            case Right(c)    => c.run(this)
            case Left(error) => error.printHelp(console)
          }
        }.getOrElse(commandNotFound(cmd))
      }
    }

    def readCmdLine: F[Option[(String, Seq[String])]] =
      console.readLine.map { line =>
        val parts = line.split(' ')
        parts.headOption.filter(_.nonEmpty).map(_ -> parts.tail)
      }

    def execute: F[Unit] = {
      val runnable = readCmdLine.flatMap {
        _.fold(F.pure(())) { case (cmd, args) => executeCmdLine(cmd, args) }
      }

      runnable.attempt.flatMap {
        case Right(_)  => F.pure(())
        case Left(err) => console.printLine(err.getMessage)
      } >> F.suspend(execute)
    }

    welcome >> execute
  }

}