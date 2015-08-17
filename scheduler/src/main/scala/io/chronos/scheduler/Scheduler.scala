package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.Trigger
import io.chronos.Trigger.Immediate
import io.chronos.id._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  case class ScheduleJob(jobId: JobId,
                         params: Map[String, AnyVal] = Map.empty,
                         trigger: Trigger = Immediate,
                         timeout: Option[FiniteDuration] = None)

}

class Scheduler(maxWorkTimeout: FiniteDuration) extends Actor with ActorLogging {
  import Scheduler._

  private val jobRegistry = context.actorOf(Props[Registry])
  private val taskDispatcher = context.actorOf(TaskQueue.props(maxWorkTimeout))

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val taskDef = TaskMeta(cmd.params, cmd.trigger, cmd.timeout)
      val schedule = context.actorOf(ExecutionPlan.props(taskDef, taskDispatcher))
      jobRegistry.tell(Registry.GetJob(cmd.jobId), schedule)
  }

}
