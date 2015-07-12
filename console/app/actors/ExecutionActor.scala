package actors

import akka.actor.Actor
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import io.chronos.protocol.SchedulerProtocol._
import io.chronos.topic
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Enumerator}

/**
 * Created by aalonsodominguez on 12/07/15.
 */
object ExecutionActor {

  case class ExecutionChannel(subscriptionId: Long,
                              enumerator: Enumerator[ExecutionEvent],
                              channel: Channel[ExecutionEvent])

  case class OpenChannel(subscriptionId: Long)
  case class CloseChannel(subscriptionId: Long)
}

class ExecutionActor extends Actor {
  import ExecutionActor._

  private val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topic.Executions, self)

  private var subscriptions: Map[Long, ExecutionChannel] = Map.empty

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
      context.become(ready, discardOld = false)
  }

  def ready: Receive = {
    case _: DistributedPubSubMediator.UnsubscribeAck =>
      context.unbecome()

    case OpenChannel(subscriptionId) =>
      val channel: ExecutionChannel = subscriptions.getOrElse(subscriptionId, {
        val broadcast: (Enumerator[ExecutionEvent], Channel[ExecutionEvent]) = Concurrent.broadcast[ExecutionEvent]
        ExecutionChannel(subscriptionId, broadcast._1, broadcast._2)
      })
      if (!subscriptions.contains(subscriptionId)) {
        subscriptions += (subscriptionId -> channel)
      }
      sender() ! channel.enumerator

    case CloseChannel(subscriptionId) =>
      require(subscriptions.contains(subscriptionId))
      subscriptions(subscriptionId).channel.end()
      subscriptions -= subscriptionId

    case event: ExecutionEvent =>
      subscriptions.values.foreach { subscription =>
        subscription.channel.push(event)
      }
  }

}
