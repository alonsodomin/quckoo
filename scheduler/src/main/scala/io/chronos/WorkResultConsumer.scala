package io.chronos

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
class WorkResultConsumer extends Actor with ActorLogging {

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(topic.AllResults, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Consumed result: {}", result)
  }

}
