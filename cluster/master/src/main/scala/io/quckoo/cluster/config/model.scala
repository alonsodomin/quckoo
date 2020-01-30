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

package io.quckoo.cluster.config

import com.typesafe.config.Config

import io.quckoo.config._
import io.quckoo.resolver.config.IvyConfig

import pureconfig._
import pureconfig.generic.auto._

import eu.timepit.refined.pureconfig._

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

final case class ClusterSettings(
    resolver: IvyConfig,
    taskQueue: TaskQueueSettings,
    http: HttpSettings
)

object ClusterSettings {
  final val Namespace = "quckoo"

  def apply(config: Config): Try[ClusterSettings] =
    Try(ConfigSource.fromConfig(config).at(Namespace).loadOrThrow)

}

final case class HttpSettings(
    bindInterface: IPv4,
    bindPort: PortNumber,
    requestTimeout: FiniteDuration
)
final case class TaskQueueSettings(maxWorkTimeout: FiniteDuration)
