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

package io.quckoo.worker.config

import com.typesafe.config.Config

import io.quckoo.resolver.ivy.IvyConfig

import pureconfig._

import scala.util.Try

/**
  * Created by alonsodomin on 02/11/2016.
  */
final case class WorkerSettings(
    contactPoints: Set[ContactPoint],
    resolver: IvyConfig,
    dispatcherName: Option[String]
)

object WorkerSettings {
  final val Namespace = "worker"

  def apply(config: Config): Try[WorkerSettings] =
    loadConfig(config, Namespace)
}