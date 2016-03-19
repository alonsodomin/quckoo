package io.kairos.cluster.scheduler

import akka.actor.{ActorLogging, ActorRef, Props, RootActorPath}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.Normal
import io.kairos.Task
import io.kairos.cluster.scheduler.TaskQueue.EnqueueAck
import io.kairos.fault.Fault
import io.kairos.id.PlanId

import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * Created by aalonsodominguez on 17/08/15.
 */
object Execution {
  import Task._

  final val DefaultEnqueueTimeout = 10 seconds

  sealed trait Command
  case class WakeUp(task: Task) extends Command
  case object Start extends Command
  case class Finish(fault: Option[Fault]) extends Command
  case class Cancel(reason: String) extends Command
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
  case object Done extends Phase {
    override def identifier = "Done"
  }

  sealed trait ExecutionEvent
  case class Awaken(task: Task) extends ExecutionEvent
  case class Cancelled(reason: String) extends ExecutionEvent
  case object Triggered extends ExecutionEvent
  case object Started extends ExecutionEvent
  case class Completed(fault: Option[Fault]) extends ExecutionEvent
  case object TimedOut extends ExecutionEvent

  case class Result(outcome: Outcome)

  final case class ExecutionState private (
      planId: PlanId,
      task: Option[Task] = None,
      outcome: Outcome = NotStarted
  ) {

    private[scheduler] def <<= (out: Outcome): ExecutionState =
      this.copy(outcome = out)

  }

  def props(planId: PlanId,
            enqueueTimeout: FiniteDuration = DefaultEnqueueTimeout,
            executionTimeout: Option[FiniteDuration] = None) =
    Props(classOf[Execution], planId, enqueueTimeout, executionTimeout)

}

class Execution(
    planId: PlanId,
    enqueueTimeout: FiniteDuration,
    executionTimeout: Option[FiniteDuration]
  ) extends PersistentFSM[Execution.Phase, Execution.ExecutionState, Execution.ExecutionEvent]
    with ActorLogging {

  import Execution._
  import Task._

  private[this] val taskQueue = context.actorSelection(
    RootActorPath(self.path.address) / "user" / "kairos" / "scheduler" / "queue"
  )
  private[this] var enqueueAttempts = 0

  startWith(Scheduled, ExecutionState(planId))

  private def sendToQueue(task: Task) = {
    taskQueue ! TaskQueue.Enqueue(task)
    stay forMax enqueueTimeout
  }

  when(Scheduled) {
    case Event(WakeUp(task), _) =>
      log.debug("Execution waking up. taskId={}", task.id)
      sendToQueue(task) applying Awaken(task)
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(Get, data) =>
      stay replying data
    case Event(EnqueueAck(taskId), data) if data.task.exists(_.id == taskId) =>
      goto(Waiting) applying Triggered
    case Event(StateTimeout, ExecutionState(_, Some(task), _))  =>
      enqueueAttempts += 1
      if (enqueueAttempts < 2) {
        sendToQueue(task)
      }
      else goto(Done) applying Cancelled(s"Could not enqueue task! taskId=${task.id}")
  }

  when(Waiting) {
    case Event(Start, ExecutionState(_, Some(task), _)) =>
      log.debug("Execution starting. taskId={}", task.id)
      val st = goto(InProgress) applying Started
      executionTimeout.map(duration => st forMax duration).getOrElse(st)
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(Get, data) =>
      stay replying data
    case Event(WakeUp(task), _) =>
      log.error("--> AAAAAAAAAAAAAAAAAAAHHHHHHHHHHHHHHHH taskId={}", task.id)
      stay
  }

  when(InProgress) {
    case Event(Get, data) =>
      stay replying data
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(Finish(result), ExecutionState(_, Some(task), _)) =>
      log.debug("Execution finishing. taskId={}", task.id)
      goto(Done) applying Completed(result)
    case Event(TimeOut, _) =>
      goto(Done) applying TimedOut
    case Event(StateTimeout, ExecutionState(_, Some(task), _)) =>
      log.debug("Execution has timed out, notifying queue. taskId={}", task.id)
      taskQueue ! TaskQueue.TimeOut(task.id)
      stay()
  }

  when(Done, stateTimeout = 10 millis) {
    case Event(Get, data) =>
      stay replying data
    case Event(StateTimeout, _) => stop()
    case Event(_, _) => stay()
  }

  onTermination {
    case StopEvent(Normal, _, data) =>
      context.parent ! Result(data.outcome)
  }

  initialize()

  override val persistenceId: String = "Execution-" + self.path.address

  override def domainEventClassTag: ClassTag[ExecutionEvent] = ClassTag(classOf[ExecutionEvent])

  override def applyEvent(event: ExecutionEvent, previous: ExecutionState): ExecutionState = event match {
    case Awaken(t) => previous.copy(task = Some(t))

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
