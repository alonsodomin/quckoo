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

package io.quckoo.api

import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.scheduler.SchedulerEvent
import io.quckoo.protocol.worker.WorkerEvent

import scala.reflect.ClassTag

/**
  * Created by alonsodomin on 20/09/2016.
  */
sealed abstract class TopicTag[A](val name: String)(implicit val eventType: ClassTag[A])
    extends Product with Serializable

object TopicTag {

  @inline def apply[A](implicit ev: TopicTag[A]): TopicTag[A] = ev

  implicit case object Master    extends TopicTag[MasterEvent]("master")
  implicit case object Worker    extends TopicTag[WorkerEvent]("worker")
  implicit case object Registry  extends TopicTag[RegistryEvent]("registry")
  implicit case object Scheduler extends TopicTag[SchedulerEvent]("scheduler")

}
