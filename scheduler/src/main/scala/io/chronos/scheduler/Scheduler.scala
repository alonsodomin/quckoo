package io.chronos.scheduler

import java.time.Clock
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
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

  def props(registryProps: Props, queueProps: Props)(implicit clock: Clock) =
    Props(classOf[Scheduler], registryProps, queueProps, clock)

}

class Scheduler(registryProps: Props, queueProps: Props)(implicit clock: Clock) extends Actor with ActorLogging {
  import RegistryProtocol._
  import SchedulerProtocol._

  private val jobRegistry = context.actorOf(registryProps, "registry")
  private val taskQueue = context.actorOf(queueProps, "taskQueue")

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      implicit val timeout = Timeout(2 seconds)
      (jobRegistry ? GetJob(cmd.jobId)) onComplete {
        case Success(response) => response match {
          case spec: JobSpec => // create execution plan
            val plan = context.actorOf(ExecutionPlan.props(cmd.trigger) { (planId, jobSpec) =>
              val task = Task(UUID.randomUUID(), jobSpec.moduleId, cmd.params, jobSpec.jobClass)
              Execution.props(planId, task, taskQueue, cmd.timeout)
            })
            plan.tell(spec, sender())

          case jne: JobNotEnabled =>
            sender() ! jne
        }

        case Failure(cause) =>
          sender() ! JobFailedToSchedule(cmd.jobId, cause)
      }
      /*
      val plan = context.actorOf(ExecutionPlan.props(cmd.trigger) { (planId, jobSpec) =>
        val task = Task(UUID.randomUUID(), jobSpec.moduleId, cmd.params, jobSpec.jobClass)
        Execution.props(planId, task, taskQueue, cmd.timeout)
      })
      jobRegistry.tell(RegistryProtocol.GetJob(cmd.jobId), plan)*/
  }

}
