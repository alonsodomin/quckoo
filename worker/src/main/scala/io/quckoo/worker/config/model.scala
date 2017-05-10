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

package io.quckoo.worker.config

import akka.actor.ActorPath
import com.typesafe.config.Config

import io.quckoo.config._
import io.quckoo.resolver.config.IvyConfig

import pureconfig._

import scala.util.Try

class ContactPoint(val actorPath: ActorPath) extends AnyVal

final case class ControllerSettings(
    contactPoints: Set[ContactPoint]
)

final case class WorkerSettings(
    worker: ControllerSettings,
    resolver: IvyConfig
)

object WorkerSettings {
  final val Namespace = "quckoo"

  def apply(config: Config): Try[WorkerSettings] =
    Try(loadConfigOrThrow[WorkerSettings](config, Namespace))
}