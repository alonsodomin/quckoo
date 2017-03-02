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

package io.quckoo.cluster.config

import com.typesafe.config.Config

import io.quckoo.config._
import io.quckoo.resolver.config.IvyConfig

import pureconfig._
import pureconfig.error.ConfigReaderFailures

import scala.concurrent.duration.FiniteDuration

final case class ClusterSettings(resolver: IvyConfig, taskQueue: TaskQueueSettings, http: HttpSettings)

object ClusterSettings {
  final val Namespace = "quckoo"

  def apply(config: Config): Either[ConfigReaderFailures, ClusterSettings] =
    loadConfig[ClusterSettings](config, Namespace)

}

final case class HttpSettings(bindInterface: String, bindPort: Int, requestTimeout: FiniteDuration)
final case class TaskQueueSettings(maxWorkTimeout: FiniteDuration)
