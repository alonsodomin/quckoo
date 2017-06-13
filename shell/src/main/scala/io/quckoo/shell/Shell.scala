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

import cats.data.StateT
import cats.effect.Effect
import cats.implicits._

import io.quckoo.Info
import io.quckoo.auth.Passport
import io.quckoo.client.QuckooClient
import io.quckoo.client.http.{Http, HttpProtocol}
import io.quckoo.client.http.akka.HttpAkkaQuckooClient
import io.quckoo.shell.console.Console

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by alonsodomin on 03/06/2017.
  */
trait Shell[F[_]] {

  def console: Console[F]

  def context: ShellContext = new ShellContext {
    override def passport: Passport = ???
  }

  //def dispatch[A](f: QuckooClient[HttpProtocol] => ClientOp[A]): ShellOp[F, A]

}

class RunnableShell[F[_]](
    val console: Console[F],
    commands: Map[String, CommandParser],
    quitCmd: String = "quit"
  )(implicit F: Effect[F], executionContext: ExecutionContext) extends Shell[F] {

  private val client: QuckooClient[HttpProtocol] = HttpAkkaQuckooClient("localhost")

  private[this] def commandNotFound(cmdName: String): ShellOp[F, Unit] =
    StateT.lift(console.printLine(s"Command not found: $cmdName"))

  private[this] def executeCmdLine(cmd: String, args: Seq[String]): ShellOp[F, Unit] = {
    if (cmd === quitCmd) Quit.run(this)
    else {
      commands.get(cmd).map { parser =>
        parser.parse(args) match {
          case Right(c)    => c.run(this)
          case Left(error) => ShellOp.lift(error.printHelp(console))
        }
      }.getOrElse(commandNotFound(cmd))
    }
  }

  def dispatch[A](f: QuckooClient[HttpProtocol] => Future[A]): ShellOp[F, A] = ShellOp.lift(F.async { handler =>
    f(client).onComplete {
      case Success(a)   => handler(Right(a))
      case Failure(err) => handler(Left(err))
    }
  })

  def runInteractive: ShellOp[F, Unit] = {
    def welcome: ShellOp[F, Unit] = ShellOp.lift(console.printLine(s"Quckoo v${Info.version}"))

    def readCmdLine: ShellOp[F, Option[(String, Seq[String])]] =
      ShellOp.lift(console.readLine.map { line =>
        val parts = line.split(' ')
        parts.headOption.filter(_.nonEmpty).map(_ -> parts.tail)
      })

    def execute: ShellOp[F, Unit] = {
      val runnable = readCmdLine.flatMap {
        _.fold(ShellOp.unit) { case (cmd, args) => executeCmdLine(cmd, args) }
      }

      runnable.attempt.flatMap {
        case Right(_)  => ShellOp.unit
        case Left(err) => ShellOp.lift(console.printLine(err.getMessage))
      } >> ShellOp.suspend(execute)
    }

    welcome >> execute
  }

}