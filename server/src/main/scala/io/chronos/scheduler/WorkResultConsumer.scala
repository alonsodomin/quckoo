package io.chronos.scheduler

import akka.actor.{ActorLogging, Actor}
import akka.contrib.pattern.{DistributedPubSubMediator, DistributedPubSubExtension}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
class WorkResultConsumer extends Actor with ActorLogging {

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(Master.ResultsTopic, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Consumed result: {}", result)
  }

}
