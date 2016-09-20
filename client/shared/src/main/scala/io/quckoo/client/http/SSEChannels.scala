package io.quckoo.client.http

import upickle.default.{Reader => UReader}

import io.quckoo.client.core.{Channel, Channels, Unmarshall}
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.registry.RegistryEvent
import io.quckoo.protocol.scheduler.SchedulerEvent
import io.quckoo.protocol.worker.WorkerEvent
import io.quckoo.serialization.json._

/**
  * Created by domingueza on 20/09/2016.
  */
trait SSEChannels extends Channels[HttpProtocol] {

  private[this] def channelOf[E: UReader](eventsUri: String): Channel.Aux[HttpProtocol, E] = new Channel.Aux[HttpProtocol, E] {
    override val uri = eventsUri
    override val unmarshall = Unmarshall[HttpServerSentEvent, E](_.data.as[E])
  }

  implicit lazy val masterCh: MasterChannel = channelOf[MasterEvent](MasterEventsURI)
  implicit lazy val workerCh: WorkerChannel = channelOf[WorkerEvent](WorkerEventsURI)
  implicit lazy val registryCh: RegistryChannel = channelOf[RegistryEvent](RegistryEventsURI)
  implicit lazy val schedulerCh: SchedulerChannel = channelOf[SchedulerEvent](SchedulerEventsURI)

}
