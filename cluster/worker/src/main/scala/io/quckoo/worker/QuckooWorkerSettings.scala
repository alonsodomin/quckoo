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

package io.quckoo.worker

import akka.actor.ActorSystem
import io.quckoo.resolver.ivy.IvyConfiguration

/**
  * Created by alonsodomin on 23/01/2016.
  */
object QuckooWorkerSettings {

  final val DefaultTcpInterface = "127.0.0.1"
  final val DefaultTcpPort      = 5001

  final val QuckooContactPoints = "contact-points"

  def apply(system: ActorSystem): QuckooWorkerSettings = {
    val sysConfig = system.settings.config

    val quckooConf = sysConfig.getConfig("quckoo")
    QuckooWorkerSettings(IvyConfiguration(quckooConf))
  }

}

case class QuckooWorkerSettings private (ivyConfiguration: IvyConfiguration)
