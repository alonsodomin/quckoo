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

package io.quckoo.cluster

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import io.quckoo.resolver.ivy.IvyConfiguration

import scala.concurrent.duration._

/**
 * Created by domingueza on 28/08/15.
 */
object QuckooClusterSettings {

  final val DefaultHttpInterface = "0.0.0.0"
  final val DefaultHttpPort = 8095

  final val DefaultTcpInterface = "127.0.0.1"
  final val DefaultTcpPort = 2551

  def apply(system: ActorSystem): QuckooClusterSettings = {
    val config = system.settings.config.getConfig(BaseConfigNamespace)
    QuckooClusterSettings(
      IvyConfiguration(config),
      config.getDuration("task-queue.max-work-timeout", TimeUnit.MILLISECONDS) millis,
      config.getString("http.bind-interface"),
      config.getInt("http.bind-port")
    )
  }

}

final case class QuckooClusterSettings private(
    ivyConfiguration: IvyConfiguration,
    queueMaxWorkTimeout: FiniteDuration,
    httpInterface: String = QuckooClusterSettings.DefaultHttpInterface,
    httpPort: Int = QuckooClusterSettings.DefaultHttpPort
)
