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

package io.quckoo.client.tcp

import akka.actor._
import akka.cluster.client.ClusterClient.Send
import akka.cluster.client.{ClusterClient, ClusterClientSettings}

import io.quckoo.protocol.client._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 21/08/15.
 */
object QuckooTcpClient {

  private[tcp] final val BasePath   = "/user/quckoo"
  private[tcp] final val SchedulerPath = BasePath + "/scheduler"
  private[tcp] final val RegistryPath =  BasePath + "/registry"

  def props(clientSettings: ClusterClientSettings, maxConnectionAttempts: Int = 3) =
    Props(classOf[QuckooTcpClient], clientSettings, maxConnectionAttempts)

}

class QuckooTcpClient(clientSettings: ClusterClientSettings, maxConnectionAttempts: Int)
  extends Actor with ActorLogging {

  import QuckooTcpClient._

  private val connectTimeout = 3 seconds

  private val clusterClient = context.watch(context.actorOf(ClusterClient.props(clientSettings), "client"))

  def receive = standby

  private def standby: Receive = {
    case Connect =>
      context.actorOf(Props(classOf[ConnectHandler], clusterClient, sender(),
        connectTimeout, maxConnectionAttempts), "connector"
      )
      context.become(connecting, discardOld = false)
  }

  private def connecting: Receive = {
    case Connected =>
      context.system.eventStream.publish(Connected)
      context.become(connected)

    case UnableToConnect =>
      context.system.eventStream.publish(UnableToConnect)
      context.unbecome()
  }

  private def connected: Receive = {
    case Disconnect =>
      clusterClient ! Send(BasePath, Disconnect, localAffinity = true)

    case Disconnected =>
      log.info("Disconnected from Quckoo cluster.")
      context.system.eventStream.publish(Disconnected)
      context.become(standby)

    case cmd: RegistryCommand =>
      val handler = context.actorOf(Props(classOf[RequestHandler], sender()))
      clusterClient.tell(Send(RegistryPath, cmd, localAffinity = true), handler)

    case cmd: SchedulerCommand =>
      val handler = context.actorOf(Props(classOf[RequestHandler], sender()))
      clusterClient.tell(Send(SchedulerPath, cmd, localAffinity = true), handler)

  }

}

private class ConnectHandler(clusterClient: ActorRef, requestor: ActorRef, timeout: FiniteDuration, maxConnectionAttempts: Int)
  extends Actor with ActorLogging {

  import QuckooTcpClient._

  private var connectionAttempts = 0
  attemptConnect()

  def receive: Receive = {
    case ReceiveTimeout =>
      if (connectionAttempts < maxConnectionAttempts) {
        log.warning("Couldn't connect with the cluster after {}. Retrying...", timeout)
        attemptConnect()
      } else {
        log.error("Couldn't connect with the cluster after {} attempts. Giving up!", connectionAttempts)
        context.parent.tell(UnableToConnect, requestor)
        context.stop(self)
      }

    case Connected =>
      log.info("Connected to Quckoo cluster at: {}", sender().path.address)
      context.parent.tell(Connected, requestor)
      context.stop(self)
  }

  private def attemptConnect(): Unit = {
    clusterClient ! Send(BasePath, Connect, localAffinity = true)
    context.setReceiveTimeout(timeout)
    connectionAttempts += 1
  }

}

private class RequestHandler(replyTo: ActorRef) extends Actor {

  def receive: Receive = {
    case msg: Any =>
      replyTo ! msg
      context.stop(self)
  }

}
