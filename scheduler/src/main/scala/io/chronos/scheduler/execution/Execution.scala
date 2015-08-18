package io.chronos.scheduler.execution

import java.util.UUID

import akka.actor.{ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
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
  case class Success(result: Any) extends Outcome
  case class Failure(cause: TaskFailureCause) extends Outcome
  case class NeverRun(reason: String) extends Outcome
  case object NeverEnding extends Outcome

  case class Result(outcome: Outcome)

  type PotentialOutcome = Option[Outcome]

  def props(planId: PlanId, task: Task, taskQueue: ActorRef) =
    Props(classOf[Execution], planId, task, taskQueue)

}

class Execution(planId: PlanId, task: Task, taskQueue: ActorRef)
  extends PersistentFSM[Execution.Phase, Execution.PotentialOutcome, Execution.DomainEvent] {

  import Execution._

  private var enqueueAttempts: Int = 0

  startWith(Sleeping, None)

  when(Sleeping) {
    case Event(WakeUp, _) =>
      goto(Waiting) applying Triggered andThen sendToQueue
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(GetOutcome, data) =>
      stay replying data
  }

  when(Waiting, stateTimeout = 10 seconds) {
    case Event(Start, _) =>
      goto(InProgress) applying Started
    case Event(Cancel(reason), _) =>
      goto(Done) applying Cancelled(reason)
    case Event(GetOutcome, data) =>
      stay replying data
    case Event(StateTimeout, _) =>
      enqueueAttempts += 1
      if (enqueueAttempts < 5) {
        stay andThen sendToQueue
      } else {
        goto(Done) applying TimedOut
      }
  }

  when(InProgress) {
    case Event(Finish(result), _) =>
      goto(Done) applying Completed(result)
    case Event(TimeOut, _) =>
      goto(Done) applying TimedOut
    case Event(GetOutcome, data) =>
      stay replying data
  }

  when(Done, stateTimeout = 1 second) {
    case Event(GetOutcome, data) =>
      stay replying data
    case Event(StateTimeout, _) => stop()
  }

  onTransition {
    case _ -> Done =>
      // Bubble up the outcome of the execution
      context.parent ! Result(stateData.get)
  }

  initialize()

  override val persistenceId: String = UUID.randomUUID().toString

  override def domainEventClassTag: ClassTag[DomainEvent] = ClassTag(classOf[DomainEvent])

  override def applyEvent(event: DomainEvent, previousOutcome: PotentialOutcome): PotentialOutcome = event match {
    case Completed(report) => report match {
      case Left(cause)   => Some(Failure(cause))
      case Right(result) => Some(Success(result))
    }
    case Cancelled(reason) => Some(NeverRun(reason))
    case TimedOut          => Some(NeverEnding)
    case _                 => previousOutcome
  }

  private def sendToQueue(potentialOutcome: PotentialOutcome): Unit = taskQueue ! TaskQueue.Enqueue(task)

}
