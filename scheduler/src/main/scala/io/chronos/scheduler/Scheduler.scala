package io.chronos.scheduler

import java.time.Clock
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.pattern._
import akka.util.Timeout
import io.chronos.JobSpec
import io.chronos.cluster.Task
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.chronos.scheduler.execution.{Execution, ExecutionPlan}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  def props(registry: ActorRef, queueProps: Props, registryTimeout: FiniteDuration = 2 seconds)(implicit clock: Clock) =
    Props(classOf[Scheduler], registry, queueProps, registryTimeout, clock)

}

class Scheduler(registry: ActorRef, queueProps: Props, registryTimeout: FiniteDuration)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import RegistryProtocol._
  import SchedulerProtocol._
  import context.dispatcher

  ClusterClientReceptionist(context.system).registerService(self)

  private val taskQueue = context.actorOf(queueProps, "taskQueue")

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      implicit val timeout = Timeout(registryTimeout)
      val origSender = sender()

      (registry ? GetJob(cmd.jobId)) onComplete {
        case Success(response) => response match {
          case spec: JobSpec => // create execution plan
            val plan = context.actorOf(ExecutionPlan.props(cmd.trigger) { (planId, jobSpec) =>
              val task = Task(UUID.randomUUID(), jobSpec.moduleId, cmd.params, jobSpec.jobClass)
              Execution.props(planId, task, taskQueue, cmd.timeout)
            })
            plan.tell(spec, origSender)

          case jne: JobNotEnabled =>
            origSender ! jne
        }

        case Failure(cause) =>
          log.error(cause, "Unexpected error from job registry.")
          origSender ! JobFailedToSchedule(cmd.jobId, cause)
      }
  }

}
