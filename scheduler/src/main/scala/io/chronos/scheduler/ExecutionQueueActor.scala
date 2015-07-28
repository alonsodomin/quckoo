package io.chronos.scheduler

import java.time.Clock

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import io.chronos.Execution
import io.chronos.id.ExecutionId
import org.apache.ignite.Ignite

/**
 * Created by aalonsodominguez on 29/07/15.
 */
object ExecutionQueueActor {

  case class Enqueue(execution: Execution)
  case class Dequeue()

}

class ExecutionQueueActor(ignite: Ignite, capacity: Int)(implicit clock: Clock) extends Actor with ActorLogging {
  import ExecutionQueueActor._

  val mediator = DistributedPubSubExtension(context.system).mediator
  
  private val executionQueue = ignite.queue[ExecutionId]("executionQueue", capacity, null)
  
  def receive = {
    case Enqueue(exec) =>
      executionQueue.put(exec.executionId)
      mediator ! DistributedPubSubMediator.Publish
  }
  
}
