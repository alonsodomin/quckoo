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
import io.quckoo.resolver.ivy.IvyConfig

import pureconfig._

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

final case class ClusterConfig(resolver: IvyConfig, taskQueue: TaskQueueConfig, http: HttpConfig)

object ClusterConfig {
  import IvyConfig._

  final val Namespace = "quckoo"

  def apply(config: Config): Try[ClusterConfig] =
    loadConfig[ClusterConfig](config, Namespace)

}

final case class HttpConfig(bindInterface: String, bindPort: Int, requestTimeout: FiniteDuration)
final case class TaskQueueConfig(maxWorkTimeout: FiniteDuration)