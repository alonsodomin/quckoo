package io.chronos.client

import akka.actor._
import akka.cluster.client.ClusterClient.Send
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import io.chronos.protocol._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 21/08/15.
 */
object ChronosClient {

  final val ChronosPath   = "/user/chronos"
  final val SchedulerPath = ChronosPath + "/scheduler"
  final val RegistryPath =  ChronosPath + "/registry"

  def props(clientSettings: ClusterClientSettings, maxConnectionAttempts: Int = 3) =
    Props(classOf[ChronosClient], clientSettings, maxConnectionAttempts)

}

class ChronosClient(clientSettings: ClusterClientSettings, maxConnectionAttempts: Int)
  extends Actor with ActorLogging {

  import ChronosClient._
  import RegistryProtocol._
  import SchedulerProtocol._

  private val connectTimeout = 3 seconds
  private var connectionAttempts = 0

  private val clusterClient = context.watch(context.actorOf(ClusterClient.props(clientSettings), "client"))
  attemptConnect()

  def receive = disconnected

  private def disconnected: Receive = {
    case msg @ Connected =>
      log.info("Connected to Chronos cluster at: {}", sender().path.address)
      context.system.eventStream.publish(msg)
      context.become(connected)

    case ReceiveTimeout =>
      if (connectionAttempts < maxConnectionAttempts) {
        log.warning("Couldn't connect with the cluster after {}. Retrying...")
        attemptConnect()
      } else {
        log.error("Couldn't connect with the cluster after {} attempts. Giving up!", connectionAttempts)
        context.system.terminate()
      }
  }

  private def connected: Receive = {
    case msg @ Disconnect =>
      clusterClient ! Send(ChronosPath, msg, localAffinity = true)

    case msg @ Disconnected =>
      log.info("Disconnected from Chronos cluster.")
      context.system.eventStream.publish(msg)
      context.become(disconnected)

    case cmd: RegistryCommand =>
      val handler = context.actorOf(Props(classOf[RequestHandler], sender()))
      clusterClient.tell(Send(RegistryPath, cmd, localAffinity = true), handler)

    case cmd: SchedulerCommand =>
      val handler = context.actorOf(Props(classOf[RequestHandler], sender()))
      clusterClient.tell(Send(SchedulerPath, cmd, localAffinity = true), handler)

    case status: ClusterStatus =>
      context.system.eventStream.publish(status)
  }

  private def attemptConnect(): Unit = {
    clusterClient ! Send(ChronosPath, Connect, localAffinity = true)
    context.setReceiveTimeout(connectTimeout)
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