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

package io.quckoo.api

import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.scheduler.SchedulerEvent
import io.quckoo.protocol.worker.WorkerEvent

/**
  * Created by alonsodomin on 20/09/2016.
  */
sealed abstract class Topic[A](val name: String)

object Topic {

  @inline def apply[A](implicit ev: Topic[A]): Topic[A] = ev

  implicit case object Master    extends Topic[MasterEvent]("MASTER_EVENT")
  implicit case object Worker    extends Topic[WorkerEvent]("WORKER_EVENT")
  implicit case object Registry  extends Topic[RegistryEvent]("REGISTRY_EVENT")
  implicit case object Scheduler extends Topic[SchedulerEvent]("SCHEDULER_EVENT")

}
