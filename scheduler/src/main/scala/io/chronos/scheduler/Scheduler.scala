package io.chronos.scheduler

import java.time.Clock

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.Trigger
import io.chronos.Trigger.Immediate
import io.chronos.id._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  def props(registryProps: Props, queueProps: Props)(implicit clock: Clock) =
    Props(classOf[Scheduler], registryProps, queueProps, clock)

  case class ScheduleJob(jobId: JobId,
                         params: Map[String, AnyVal] = Map.empty,
                         trigger: Trigger = Immediate,
                         timeout: Option[FiniteDuration] = None)

}

class Scheduler(registryProps: Props, queueProps: Props)(implicit clock: Clock) extends Actor with ActorLogging {
  import Scheduler._

  private val jobRegistry = context.actorOf(registryProps, "registry")
  private val taskQueue = context.actorOf(queueProps, "queue")

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val taskDef = TaskMeta(cmd.params, cmd.trigger, cmd.timeout)
      val plan = context.actorOf(ExecutionPlan.props(taskDef, taskQueue))
      jobRegistry.tell(Registry.GetJob(cmd.jobId), plan)
  }

}
