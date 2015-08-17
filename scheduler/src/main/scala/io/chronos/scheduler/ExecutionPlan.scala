package io.chronos.scheduler

import java.time.{Clock, Duration => JDuration, ZonedDateTime}
import java.util.UUID

import akka.actor._
import io.chronos.JobSpec
import io.chronos.Trigger._
import io.chronos.cluster.Task
import io.chronos.id._

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object ExecutionPlan {

  def props(taskMeta: TaskMeta, taskQueue: ActorRef)(implicit clock: Clock) =
    Props(classOf[ExecutionPlan], taskMeta, clock)

}

class ExecutionPlan(taskMeta: TaskMeta, taskQueue: ActorRef)(implicit clock: Clock)
  extends Actor with ActorLogging {

  private val planId: PlanId = UUID.randomUUID()
  private var triggerTask: Option[Cancellable] = None
  private var lastExecutionTime: Option[ZonedDateTime] = None

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[JobDisabled])
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  override def receive: Receive = {
    case jobSpec: JobSpec =>
      val task = Task(newTaskId, jobSpec.moduleId, taskMeta.params, jobSpec.jobClass)
      context.become(scheduleTrigger(jobSpec.id, task))

    case JobNotEnabled(_) =>
      log.info("Job can't be scheduled")
      self ! PoisonPill
  }

  private def active(jobId: JobId, task: Task): Receive = {
    case JobDisabled(id) if id == jobId =>
      log.info("Job has been disabled. jobId={}", id)
      triggerTask.foreach { _.cancel() }
      triggerTask = None
      self ! PoisonPill
      context.become(inactive)

    case Execution.Result(outcome) =>
      lastExecutionTime = Some(ZonedDateTime.now(clock))
      if (taskMeta.trigger.isRecurring) {
        outcome match {
          case _: Execution.Success =>
            context.become(scheduleTrigger(jobId, task))
        }
      }
  }

  private def inactive: Receive = {
    case _ => self ! PoisonPill
  }

  private def scheduleTrigger(jobId: JobId, task: Task): Receive = triggerDelay match {
    case Some(delay) =>
      // Create a new execution
      val execution = context.actorOf(Execution.props(planId, task, taskQueue))
      triggerTask = Some(context.system.scheduler.scheduleOnce(delay, execution, Execution.WakeUp))
      active(jobId, task)
    case _ =>
      self ! PoisonPill
      inactive
  }

  private def newTaskId: TaskId = UUID.randomUUID()

  private def triggerDelay: Option[FiniteDuration] = {
    val now = ZonedDateTime.now(clock)
    nextExecutionTime match {
      case Some(time) if time.isBefore(now) || time.isEqual(now) =>
        Some(0 millis)
      case Some(time) =>
        val delay = JDuration.between(now, time)
        Some(delay.toMillis millis)
      case None => None
    }
  }

  private def nextExecutionTime: Option[ZonedDateTime] = taskMeta.trigger.nextExecutionTime(lastExecutionTime match {
    case Some(time) => LastExecutionTime(time)
    case None       => ScheduledTime(ZonedDateTime.now(clock))
  })

}
