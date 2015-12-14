package io.kairos.cluster.core

import akka.actor.Actor.Receive
import akka.actor.Props
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberUp, ReachabilityEvent, MemberEvent, InitialStateAsEvents}
import akka.cluster.pubsub.{DistributedPubSubMediator, DistributedPubSub}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import de.heikoseeberger.akkasse.ServerSentEvent
import io.kairos.cluster.protocol.WorkerProtocol
import io.kairos.cluster.protocol.WorkerProtocol.{WorkerEvent, WorkerJoined}

import scala.annotation.tailrec

/**
  * Created by alonsodomin on 14/12/2015.
  */
object KairosEventEmitter {

  private val MaxEvents = 50

  def props: Props = Props[KairosEventEmitter]

}

class KairosEventEmitter extends ActorPublisher[KairosClusterEvent] {
  import KairosEventEmitter._
  import scala.language.implicitConversions

  private val cluster = Cluster(context.system)
  private val mediator = DistributedPubSub(context.system).mediator

  var eventBuffer = Vector.empty[KairosClusterEvent]

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[ReachabilityEvent])
    mediator ! DistributedPubSubMediator.Subscribe(WorkerProtocol.WorkerTopic, self)
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    mediator ! DistributedPubSubMediator.Unsubscribe(WorkerProtocol.WorkerTopic, self)
  }

  override def receive: Receive = {
    case evt: MemberEvent =>
      emitEvent(evt)

    case evt: WorkerEvent =>
      emitEvent(evt)

    case Request(_) =>
      deliverEvents()

    case Cancel =>
      context.stop(self)
  }

  private[this] def emitEvent(event: KairosClusterEvent): Unit = {
    if (eventBuffer.isEmpty && totalDemand > 0) {
      onNext(event)
    } else {
      if (eventBuffer.size == MaxEvents) {
        eventBuffer = eventBuffer.drop(1)
      }
      eventBuffer :+= event
      deliverEvents()
    }
  }

  private[this] implicit def toKairosClusterEvent(evt: MemberEvent): KairosClusterEvent =
    KairosClusterEvent()

  private[this] implicit def toKairosClusterEvent(evt: WorkerEvent): KairosClusterEvent =
    KairosClusterEvent()

  @tailrec
  private[this] def deliverEvents(): Unit = if (totalDemand > 0) {
    def dispatchBufferItems(itemCount: Int): Unit = {
      val (head, tail) = eventBuffer.splitAt(itemCount)
      eventBuffer = tail
      head foreach onNext
    }

    val dispatchCount = if (totalDemand <= Int.MaxValue)
        totalDemand.toInt
      else
        Int.MaxValue

    dispatchBufferItems(dispatchCount)
    if (dispatchCount == Int.MaxValue) deliverEvents()
  }

}
