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

/**
  * Created by alonsodomin on 03/06/2017.
  */

trait Command {
  def run[F[_]](shell: Shell[F])(implicit F: Sync[F]): F[Unit]
}

trait CommandParser {
  def commandName: String

  def parse(args: Seq[String]): Either[CommandParseError, Command]
}

object Quit extends Command {
  override def run[F[_]](shell: Shell[F])(implicit F: Sync[F]): F[Unit] = {
    shell.console.printLine("Bye!") >> F.delay(sys.exit())
  }
}