package io.chronos.scheduler.execution

import java.util.UUID

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.Normal
import io.chronos.cluster.{Task, TaskFailureCause}
import io.chronos.id.PlanId
import io.chronos.scheduler._
import io.chronos.scheduler.queue.TaskQueue

import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * Created by aalonsodominguez on 17/08/15.
 */
object Execution {

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
  case object NoOutcomeYet extends Outcome
  case class Success(result: Any) extends Outcome
  case class Failure(cause: TaskFailureCause) extends Outcome
  case class NeverRun(reason: String) extends Outcome
  case object NeverEnding extends Outcome

  case class Result(outcome: Outcome)

  def props(planId: PlanId, task: Task, taskQueue: ActorRef) =
    Props(classOf[Execution], planId, task, taskQueue)

}

class Execution(planId: PlanId, task: Task, taskQueue: ActorRef)
  extends PersistentFSM[Execution.Phase, Execution.Outcome, Execution.DomainEvent] with ActorLogging {

  import Execution._

  startWith(Sleeping, NoOutcomeYet)

  when(Sleeping) {
    case Event(WakeUp, _) =>
      log.info("Execution waking up...")
      goto(Waiting) applying Triggered andThen sendToQueue
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(GetOutcome, data) =>
      stay replying data
  }

  when(Waiting) {
    case Event(Start, _) =>
      log.info("Execution starting...")
      goto(InProgress) applying Started
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(GetOutcome, data) =>
      stay replying data
  }

  when(InProgress) {
    case Event(GetOutcome, data) =>
      log.info("Returning outcome: {}", data)
      stay replying data
    case Event(Finish(result), _) =>
      log.info("Execution finishing...")
      goto(Done) applying Completed(result)
    case Event(TimeOut, _) =>
      goto(Done) applying TimedOut
  }

  when(Done, stateTimeout = 500 millis) {
    case Event(GetOutcome, data) =>
      stay replying data
    case Event(StateTimeout, _) => stop()
    case Event(_, _) => stay()
  }

  onTermination {
    case StopEvent(Normal, _, data) => context.parent ! data
  }

  initialize()

  override val persistenceId: String = UUID.randomUUID().toString

  override def domainEventClassTag: ClassTag[DomainEvent] = ClassTag(classOf[DomainEvent])

  override def applyEvent(event: DomainEvent, previousOutcome: Outcome): Outcome = event match {
    case Completed(report) => report match {
      case Left(cause)   => Failure(cause)
      case Right(result) => Success(result)
    }
    case Cancelled(reason) => NeverRun(reason)
    case TimedOut          => NeverEnding
    case _                 => previousOutcome
  }

  private def sendToQueue(potentialOutcome: Outcome): Unit = taskQueue ! TaskQueue.Enqueue(task)

}
