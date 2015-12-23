package io.kairos.client

import akka.actor._
import akka.cluster.client.ClusterClient.Send
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import io.kairos.protocol._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 21/08/15.
 */
object KairosClient {

  private[client] final val KairosPath   = "/user/kairos"
  private[client] final val SchedulerPath = KairosPath + "/scheduler"
  private[client] final val RegistryPath =  KairosPath + "/registry"

  def props(clientSettings: ClusterClientSettings, maxConnectionAttempts: Int = 3) =
    Props(classOf[KairosClient], clientSettings, maxConnectionAttempts)

}

class KairosClient(clientSettings: ClusterClientSettings, maxConnectionAttempts: Int)
  extends Actor with ActorLogging {

  import KairosClient._
  import ClientProtocol._
  import RegistryProtocol._
  import SchedulerProtocol._

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
      clusterClient ! Send(KairosPath, Disconnect, localAffinity = true)

    case Disconnected =>
      log.info("Disconnected from Kaitos cluster.")
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

  import ClientProtocol._
  import KairosClient._

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
      log.info("Connected to Kairos cluster at: {}", sender().path.address)
      context.parent.tell(Connected, requestor)
      context.stop(self)
  }

  private def attemptConnect(): Unit = {
    clusterClient ! Send(KairosPath, Connect, localAffinity = true)
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