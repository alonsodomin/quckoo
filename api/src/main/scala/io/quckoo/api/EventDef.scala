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

  implicit object MasterEventDef extends EventDef[MasterEvent]("MASTER_EVENT")
  implicit object WorkerEventDef extends EventDef[WorkerEvent]("WORKER_EVENT")
  implicit object RegistryEventDef extends EventDef[RegistryEvent]("REGISTRY_EVENT")
  implicit object SchedulerEventDef extends EventDef[SchedulerEvent]("SCHEDULER_EVENT")
}
