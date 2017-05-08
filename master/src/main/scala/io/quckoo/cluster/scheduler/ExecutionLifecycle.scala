/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.scheduler

import akka.actor.{ActorSelection, Props}
import akka.persistence.fsm.PersistentFSM.Normal
import akka.persistence.fsm.{LoggingPersistentFSM, PersistentFSM}

import io.quckoo.{QuckooError, PlanId, Task, TaskExecution}
import io.quckoo.cluster.scheduler.TaskQueue.EnqueueAck

import kamon.trace.Tracer

import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
  * Created by aalonsodominguez on 17/08/15.
  */
object ExecutionLifecycle {
  import TaskExecution._

  final val DefaultEnqueueTimeout     = 10 seconds
  final val DefaultMaxEnqueueAttempts = 3

  final val PersistenceIdPrefix = "Execution-"

  sealed trait Command
  final case class Awake(task: Task, queue: ActorSelection) extends Command
  case object Start                                         extends Command
  final case class Finish(fault: Option[QuckooError])             extends Command
  final case class Cancel(reason: Reason)        extends Command
  case object TimeOut                                       extends Command
  case object Get                                           extends Command

  sealed trait Phase extends PersistentFSM.FSMState
  case object Sleeping extends Phase {
    override val identifier = "Sleeping"
  }
  case object Enqueuing extends Phase {
    override val identifier = "Enqueuing"
  }
  case object Waiting extends Phase {
    override val identifier = "Waiting"
  }
  case object Running extends Phase {
    override val identifier = "Running"
  }

  sealed trait ExecutionEvent
  final case class Awaken(task: Task, planId: PlanId, queue: ActorSelection) extends ExecutionEvent
  final case class Cancelled(reason: Reason)                      extends ExecutionEvent
  final case class Triggered(task: Task)                                     extends ExecutionEvent
  case object Started                                                        extends ExecutionEvent
  final case class Completed(fault: Option[QuckooError])                           extends ExecutionEvent
  case object TimedOut                                                       extends ExecutionEvent

  final case class Result(outcome: Outcome)

  final case class ExecutionState private (
      planId: PlanId,
      task: Option[Task] = None,
      queue: Option[ActorSelection] = None,
      outcome: Option[Outcome] = None
  ) {

    private[scheduler] def becomes(out: Outcome): ExecutionState =
      this.copy(outcome = Some(out))

  }

  def props(planId: PlanId,
            enqueueTimeout: FiniteDuration = DefaultEnqueueTimeout,
            maxEnqueueAttempts: Int = DefaultMaxEnqueueAttempts,
            executionTimeout: Option[FiniteDuration] = None): Props =
    Props(new ExecutionLifecycle(
      planId,
      enqueueTimeout,
      maxEnqueueAttempts,
      executionTimeout))

}

class ExecutionLifecycle(
    planId: PlanId,
    enqueueTimeout: FiniteDuration,
    maxEnqueueAttempts: Int,
    executionTimeout: Option[FiniteDuration]
) extends PersistentFSM[
      ExecutionLifecycle.Phase,
      ExecutionLifecycle.ExecutionState,
      ExecutionLifecycle.ExecutionEvent] with LoggingPersistentFSM[
      ExecutionLifecycle.Phase,
      ExecutionLifecycle.ExecutionState,
      ExecutionLifecycle.ExecutionEvent] {

  import ExecutionLifecycle._
  import TaskExecution._

  private[this] var enqueueAttempts = 0

  startWith(Sleeping, ExecutionState(planId))

  when(Sleeping) {
    case Event(Awake(task, queue), _) =>
      log.debug("Execution for task '{}' waking up.", task.id)
      goto(Enqueuing) applying Awaken(task, planId, queue) forMax enqueueTimeout

    case Event(Cancel(reason), data) =>
      log.debug("Cancelling execution upon request. Reason: {}", reason)
      stop applying Cancelled(reason)

    case Event(Get, ExecutionState(_, Some(task), _, outcome)) =>
      stay replying TaskExecution(planId, task, Status.Scheduled, outcome)
  }

  when(Enqueuing) {
    case Event(EnqueueAck(taskId), ExecutionState(_, Some(task), _, _)) if taskId == task.id =>
      log.debug("Queue has accepted task '{}'.", taskId)
      goto(Waiting) applying Triggered(task)

    case Event(Get, ExecutionState(_, Some(task), _, outcome)) =>
      stay replying TaskExecution(planId, task, Status.Scheduled, outcome) forMax enqueueTimeout

    case Event(StateTimeout, ExecutionState(_, Some(task), Some(queue), _)) =>
      enqueueAttempts += 1
      if (enqueueAttempts < maxEnqueueAttempts) {
        log.debug("Task '{}' failed to be enqueued. Retrying...", task.id)
        queue ! TaskQueue.Enqueue(task)
        stay forMax enqueueTimeout
      } else {
        log.debug("Task '{}' failed to be enqueued after {} attempts.", task.id, enqueueAttempts)
        stop applying Cancelled(Reason.FailedToEnqueue)
      }
  }

  when(Waiting) {
    case Event(Start, ExecutionState(_, Some(task), _, _)) =>
      log.info("Execution of task '{}' starting", task.id)
      val st = goto(Running) applying Started
      executionTimeout.map(duration => st forMax duration).getOrElse(st)

    case Event(Cancel(reason), data) =>
      log.debug(
        "Cancelling execution of task '{}' upon request. Reason: {}",
        data.task.get.id,
        reason)
      stop applying Cancelled(reason)

    case Event(Get, ExecutionState(_, Some(task), _, outcome)) =>
      stay replying TaskExecution(planId, task, Status.Enqueued, outcome)

    case Event(EnqueueAck(taskId), ExecutionState(_, Some(task), _, _)) if taskId == task.id =>
      // May happen after recovery
      stay
  }

  when(Running) {
    case Event(Get, ExecutionState(_, Some(task), _, outcome)) =>
      stay replying TaskExecution(planId, task, Status.InProgress, outcome)

    case Event(Cancel(reason), _) =>
      log.debug("Cancelling execution upon request. Reason: {}", reason)
      stop applying Cancelled(reason)

    case Event(Finish(result), ExecutionState(_, Some(task), _, _)) =>
      log.debug("Execution for task '{}' finishing.", task.id)
      stop applying Completed(result)

    case Event(TimeOut, _) =>
      stop applying TimedOut

    case Event(StateTimeout, ExecutionState(_, Some(task), Some(queue), _)) =>
      log.debug("Execution for task '{}' has timed out, notifying queue.", task.id)
      queue ! TaskQueue.TimeOut(task.id)
      stay
  }

  onTermination {
    case StopEvent(Normal, _, ExecutionState(_, _, _, Some(outcome))) =>
      context.parent ! Result(outcome)
  }

  override val persistenceId: String = PersistenceIdPrefix + self.path.name

  override def domainEventClassTag: ClassTag[ExecutionEvent] = ClassTag(classOf[ExecutionEvent])

  override def applyEvent(event: ExecutionEvent, previous: ExecutionState): ExecutionState =
    event match {
      case Awaken(task, `planId`, queue) =>
        Tracer.withNewContext(s"task-${task.id}") {
          log.debug(
            "Execution lifecycle for task '{}' is allocating a slot at the local queue.",
            task.id)
          queue ! TaskQueue.Enqueue(task)
        }
        previous.copy(task = Some(task), queue = Some(queue))

      case event @ Triggered(task) =>
        log.debug("Execution for task '{}' has been triggered.", task.id)
        context.parent ! event
        previous

      case Completed(result) =>
        result match {
          case Some(fault) => previous becomes Outcome.Failure(fault)
          case _           => previous becomes Outcome.Success
        }

      case Cancelled(reason) =>
        stateName match {
          case Running => previous becomes Outcome.Interrupted(reason)
          case _       => previous becomes Outcome.NeverRun(reason)
        }

      case TimedOut => previous becomes Outcome.NeverEnding
      case _        => previous
    }

}
