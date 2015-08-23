package io.chronos.scheduler

import java.time.Clock
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
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

  def props(registryProps: Props, queueProps: Props, registryTimeout: FiniteDuration = 2 seconds)(implicit clock: Clock) =
    Props(classOf[Scheduler], registryProps, queueProps, registryTimeout, clock)

}

class Scheduler(registryProps: Props, queueProps: Props, registryTimeout: FiniteDuration)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import RegistryProtocol._
  import SchedulerProtocol._
  import context.dispatcher

  ClusterClientReceptionist(context.system).registerService(self)

  private val registry = context.actorOf(registryProps, "registry")
  private val taskQueue = context.actorOf(queueProps, "taskQueue")

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      implicit val timeout = Timeout(registryTimeout)
      val origSender = sender()

      (registry ? GetJob(cmd.jobId)) onComplete {
        case Success(response) => response match {
          case spec: JobSpec => // create execution plan
            log.info("Scheduling job {} with following details: {}", cmd.jobId, cmd)
            val planId = UUID.randomUUID()
            val plan = context.actorOf(ExecutionPlan.props(planId, cmd.trigger) { (taskId, jobSpec) =>
              val task = Task(taskId, jobSpec.moduleId, cmd.params, jobSpec.jobClass)
              Execution.props(planId, task, taskQueue, executionTimeout = cmd.timeout)
            }, "plan-" + planId)
            plan.tell(cmd.jobId -> spec, origSender)

          case jne: JobNotEnabled =>
            origSender ! jne
        }

        case Failure(cause) =>
          log.error(cause, "Unexpected error from job registry.")
          origSender ! JobFailedToSchedule(cmd.jobId, cause)
      }
  }

}
