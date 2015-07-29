package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import io.chronos.id.ExecutionId
import io.chronos.protocol.SchedulerProtocol.ExecutionEvent
import io.chronos.{Execution, topic}
import org.apache.ignite.Ignite

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 29/07/15.
 */
object ExecutionQueueActor {

  case class Enqueue(execution: Execution)
  case object Dequeue

  case object PendingExecutionsCount
}

class ExecutionQueueActor(ignite: Ignite, capacity: Int) extends Actor with ActorLogging {
  import ExecutionQueueActor._

  val mediator = DistributedPubSubExtension(context.system).mediator
  
  private val executionQueue = ignite.queue[ExecutionId]("executionQueue", capacity, null)
  
  def receive = {
    case Enqueue(exec) =>
      executionQueue.put(exec.executionId)
      mediator ! DistributedPubSubMediator.Publish(topic.Executions, ExecutionEvent(exec.executionId, exec.stage))

    case Dequeue =>
      sender ! (
        if (executionQueue.nonEmpty)
          Some(executionQueue.take())
        else
          None
      )

    case PendingExecutionsCount =>
      sender ! executionQueue.size()
  }
  
}
