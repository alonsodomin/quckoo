package io.quckoo.client.core

import upickle.default.{Reader => UReader}

import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.scheduler.SchedulerEvent
import io.quckoo.protocol.worker.WorkerEvent

/**
  * Created by domingueza on 20/09/2016.
  */
trait Channels[P <: Protocol] {

  type MasterChannel    = Channel.Aux[P, MasterEvent]
  type WorkerChannel    = Channel.Aux[P, WorkerEvent]
  type RegistryChannel  = Channel.Aux[P, RegistryEvent]
  type SchedulerChannel = Channel.Aux[P, SchedulerEvent]

  def createChannel[E: EventDef : UReader]: Channel.Aux[P, E]

  /*implicit def masterCh: MasterChannel
  implicit def workerCh: WorkerChannel
  implicit def registryCh: RegistryChannel
  implicit def schedulerCh: SchedulerChannel*/

}
