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
sealed abstract class EventDef[A](val typeName: String)

object EventDef {

  @inline def apply[A](implicit ev: EventDef[A]) = ev

  implicit object MasterEventDef    extends EventDef[MasterEvent]("MASTER_EVENT")
  implicit object WorkerEventDef    extends EventDef[WorkerEvent]("WORKER_EVENT")
  implicit object RegistryEventDef  extends EventDef[RegistryEvent]("REGISTRY_EVENT")
  implicit object SchedulerEventDef extends EventDef[SchedulerEvent]("SCHEDULER_EVENT")
}
