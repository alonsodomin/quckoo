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

import io.quckoo.Task
import io.quckoo.cluster.scheduler.TaskQueue.EnqueueAck
import io.quckoo.fault.Fault
import io.quckoo.id.PlanId

import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * Created by aalonsodominguez on 17/08/15.
 */
object Execution {
  import Task._

  final val DefaultEnqueueTimeout = 10 seconds
  final val DefaultMaxEnqueueAttempts = 3

  sealed trait Command
  final case class WakeUp(task: Task, queue: ActorSelection) extends Command
  case object Start extends Command
  final case class Finish(fault: Option[Fault]) extends Command
  final case class Cancel(reason: UncompleteReason) extends Command
  case object TimeOut extends Command
  case object Get extends Command

  sealed trait Phase extends PersistentFSM.FSMState
  case object Scheduled extends Phase {
    override def identifier = "Scheduled"
  }
  case object Waiting extends Phase {
    override def identifier = "Waiting"
  }
  case object InProgress extends Phase {
    override def identifier = "InProgress"
  }

  sealed trait ExecutionEvent
  final case class Awaken(task: Task, queue: ActorSelection) extends ExecutionEvent
  final case class Cancelled(reason: UncompleteReason) extends ExecutionEvent
  case object Triggered extends ExecutionEvent
  case object Started extends ExecutionEvent
  final case class Completed(fault: Option[Fault]) extends ExecutionEvent
  case object TimedOut extends ExecutionEvent

  final case class Result(outcome: Outcome)

  final case class ExecutionState private (
      planId: PlanId,
      task: Option[Task] = None,
      queue: Option[ActorSelection] = None,
      outcome: Outcome = NotStarted
  ) {

    private[scheduler] def <<= (out: Outcome): ExecutionState =
      this.copy(outcome = out)

  }

  def props(planId: PlanId,
            enqueueTimeout: FiniteDuration = DefaultEnqueueTimeout,
            maxEnqueueAttempts: Int = DefaultMaxEnqueueAttempts,
            executionTimeout: Option[FiniteDuration] = None) =
    Props(classOf[Execution], planId, enqueueTimeout, maxEnqueueAttempts, executionTimeout)

}

class Execution(
    planId: PlanId,
    enqueueTimeout: FiniteDuration,
    maxEnqueueAttempts: Int,
    executionTimeout: Option[FiniteDuration]
  ) extends PersistentFSM[Execution.Phase, Execution.ExecutionState, Execution.ExecutionEvent]
    with LoggingPersistentFSM[Execution.Phase, Execution.ExecutionState, Execution.ExecutionEvent] {

  import Execution._
  import Task._

  private[this] var enqueueAttempts = 0

  startWith(Scheduled, ExecutionState(planId))

  when(Scheduled) {
    case Event(WakeUp(task, queue), _) =>
      log.debug("Execution waking up. taskId={}", task.id)
      queue ! TaskQueue.Enqueue(task)
      stay applying Awaken(task, queue) forMax enqueueTimeout

    case Event(Cancel(reason), data) =>
      log.debug("Cancelling execution upon request. Reason: {}", reason)
      stop applying Cancelled(reason)

    case Event(Get, data) =>
      stay replying data

    case Event(EnqueueAck(taskId), ExecutionState(_, Some(task), _, _)) if taskId == task.id =>
      log.debug("Queue has accepted task {}.", taskId)
      goto(Waiting) applying Triggered andThen { _ =>
        context.parent ! Triggered
      }

    case Event(StateTimeout, ExecutionState(_, Some(task), Some(queue), _))  =>
      enqueueAttempts += 1
      if (enqueueAttempts < maxEnqueueAttempts) {
        queue ! TaskQueue.Enqueue(task)
        stay forMax enqueueTimeout
      } else {
        stop applying Cancelled(FailedToEnqueue)
      }
  }

  when(Waiting) {
    case Event(Start, ExecutionState(_, Some(task), _, _)) =>
      log.info("Execution of task {} starting", task.id)
      val st = goto(InProgress) applying Started
      executionTimeout.map(duration => st forMax duration).getOrElse(st)

    case Event(Cancel(reason), data) =>
      log.debug("Cancelling execution of task {} upon request. Reason: {}", data.task.get.id, reason)
      stop applying Cancelled(reason)

    case Event(Get, data) =>
      stay replying data
  }

  when(InProgress) {
    case Event(Get, data) =>
      stay replying data

    case Event(Cancel(reason), _) =>
      log.debug("Cancelling execution upon request. Reason: {}", reason)
      stop applying Cancelled(reason)

    case Event(Finish(result), ExecutionState(_, Some(task), _, _)) =>
      log.debug("Execution finishing. taskId={}", task.id)
      stop applying Completed(result)

    case Event(TimeOut, _) =>
      stop applying TimedOut

    case Event(StateTimeout, ExecutionState(_, Some(task), Some(queue), _)) =>
      log.debug("Execution has timed out, notifying queue. taskId={}", task.id)
      queue ! TaskQueue.TimeOut(task.id)
      stay
  }

  onTermination {
    case StopEvent(Normal, _, data) =>
      context.parent ! Result(data.outcome)
  }

  override val persistenceId: String = "Execution-" + self.path.name

  override def domainEventClassTag: ClassTag[ExecutionEvent] = ClassTag(classOf[ExecutionEvent])

  override def applyEvent(event: ExecutionEvent, previous: ExecutionState): ExecutionState = event match {
    case Awaken(t, q) =>
      log.debug("Execution for task {} has awaken.", t.id)
      previous.copy(task = Some(t), queue = Some(q))

    case Completed(result) => result match {
      case Some(fault) => previous <<= Failure(fault)
      case _           => previous <<= Success
    }

    case Cancelled(reason) => stateName match {
      case InProgress => previous <<= Interrupted(reason)
      case _          => previous <<= NeverRun(reason)
    }

    case TimedOut => previous <<= NeverEnding
    case _        => previous
  }

}
