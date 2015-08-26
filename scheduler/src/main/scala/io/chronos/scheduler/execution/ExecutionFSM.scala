package io.chronos.scheduler.execution

import java.util.UUID

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.Normal
import io.chronos.cluster.{Task, TaskFailureCause}
import io.chronos.id.PlanId
import io.chronos.scheduler.TaskQueue.EnqueueAck
import io.chronos.scheduler._

import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * Created by aalonsodominguez on 17/08/15.
 */
object ExecutionFSM {

  final val DefaultEnqueueTimeout = 10 seconds

  case object WakeUp
  case object Start
  case class Finish(result: TaskResult)
  case class Cancel(reason: String)
  case object TimeOut
  case object GetOutcome

  sealed trait Phase extends PersistentFSM.FSMState
  object Sleeping extends Phase {
    override def identifier = "Sleeping"
  }
  object Waiting extends Phase {
    override def identifier = "Waiting"
  }
  object InProgress extends Phase {
    override def identifier = "InProgress"
  }
  object Done extends Phase {
    override def identifier = "Done"
  }

  sealed trait DomainEvent
  case class Cancelled(reason: String) extends DomainEvent
  case object Triggered extends DomainEvent
  case object Started extends DomainEvent
  case class Completed(result: TaskResult) extends DomainEvent
  case object TimedOut extends DomainEvent

  sealed trait Outcome
  case object NotRunYet extends Outcome
  case class Success(result: Any) extends Outcome
  case class Failure(cause: TaskFailureCause) extends Outcome
  case class NeverRun(reason: String) extends Outcome
  case object NeverEnding extends Outcome
  case class Interrupted(reason: String) extends Outcome

  case class Result(outcome: Outcome)

  def props(planId: PlanId, task: Task, taskQueue: ActorRef,
            enqueueTimeout: FiniteDuration = DefaultEnqueueTimeout,
            executionTimeout: Option[FiniteDuration] = None) =
    Props(classOf[ExecutionFSM], planId, task, taskQueue, enqueueTimeout, executionTimeout)

}

class ExecutionFSM(planId: PlanId, task: Task, taskQueue: ActorRef,
                   enqueueTimeout: FiniteDuration,
                   executionTimeout: Option[FiniteDuration])
  extends PersistentFSM[ExecutionFSM.Phase, ExecutionFSM.Outcome, ExecutionFSM.DomainEvent] with ActorLogging {

  import ExecutionFSM._

  private var enqueueAttempts = 0

  startWith(Sleeping, NotRunYet)

  when(Sleeping) {
    case Event(WakeUp, _) =>
      log.debug("Execution waking up. taskId={}", task.id)
      sendToQueue()
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(GetOutcome, data) =>
      stay replying data
    case Event(EnqueueAck(taskId), _) if taskId == task.id =>
      goto(Waiting) applying Triggered
    case Event(StateTimeout, _) =>
      enqueueAttempts += 1
      if (enqueueAttempts < 2) sendToQueue()
      else goto(Done) applying Cancelled(s"Could not enqueue task! taskId=${task.id}")
  }

  when(Waiting) {
    case Event(Start, _) =>
      log.debug("Execution starting. taskId={}", task.id)
      startExecution()
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(GetOutcome, data) =>
      stay replying data
  }

  when(InProgress) {
    case Event(GetOutcome, data) =>
      stay replying data
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(Finish(result), _) =>
      log.debug("Execution finishing. taskId={}", task.id)
      goto(Done) applying Completed(result)
    case Event(TimeOut, _) =>
      goto(Done) applying TimedOut
    case Event(StateTimeout, _) =>
      log.debug("Execution has timed out, notifying queue. taskId={}", task.id)
      taskQueue ! TaskQueue.TimeOut(task.id)
      stay()
  }

  when(Done, stateTimeout = 10 millis) {
    case Event(GetOutcome, data) =>
      stay replying data
    case Event(StateTimeout, _) => stop()
    case Event(_, _) => stay()
  }

  onTermination {
    case StopEvent(Normal, _, data) => context.parent ! Result(data)
  }

  initialize()

  override val persistenceId: String = UUID.randomUUID().toString

  override def domainEventClassTag: ClassTag[DomainEvent] = ClassTag(classOf[DomainEvent])

  override def applyEvent(event: DomainEvent, previousOutcome: Outcome): Outcome = event match {
    case Completed(report) => report match {
      case Left(cause)   => Failure(cause)
      case Right(result) => Success(result)
    }
    case Cancelled(reason) => stateName match {
      case InProgress => Interrupted(reason)
      case _          => NeverRun(reason)
    }
    case TimedOut          => NeverEnding
    case _                 => previousOutcome
  }

  private def startExecution() = {
    val st = goto(InProgress) applying Started
    executionTimeout.map( duration => st forMax duration ).getOrElse(st)
  }

  private def sendToQueue() = {
    taskQueue ! TaskQueue.Enqueue(task)
    stay forMax enqueueTimeout
  }

}
