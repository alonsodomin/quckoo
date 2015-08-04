package io.chronos

import java.time.{Clock, ZonedDateTime}

import io.chronos.id._
import io.chronos.protocol.ExecutionFailedCause

import scala.language.implicitConversions

/**
 * Created by aalonsodominguez on 11/07/15.
 */
object Execution {

  def apply(executionId: ExecutionId)(implicit c: Clock): Execution =
    new Execution(executionId) << Scheduled(ZonedDateTime.now(c))

  sealed abstract class Stage(private val ordinal: Int) extends Ordered[Stage] with Serializable {
    implicit val when: ZonedDateTime

    override def compare(that: Stage): Int = ordinal compare that.ordinal

  }

  sealed trait StageLike[T <: Stage] { self =>

     def matches[S <: Stage](stage: S): Boolean

    final def currentIn(execution: Execution): Boolean = self.matches(execution.stage)

    final def grab(execution: Execution): Option[T] = execution.stages.
      find(self.matches).
      map { _.asInstanceOf[T] }

  }

  sealed trait WorkerStage {
    implicit val where: WorkerId
  }

  case class Scheduled(when: ZonedDateTime) extends Stage(1)
  case class Triggered(when: ZonedDateTime) extends Stage(2)
  case class Started(when: ZonedDateTime, where: WorkerId) extends Stage(3) with WorkerStage
  case class Finished(when: ZonedDateTime, where: WorkerId, outcome: Outcome) extends Stage(4) with WorkerStage

  implicit case object Ready extends StageLike[Scheduled] {
    override def matches[S <: Stage](stage: S): Boolean = stage match {
      case _: Scheduled => true
      case _            => false
    }
  }
  implicit case object Waiting extends StageLike[Triggered] {
    override def matches[S <: Stage](stage: S): Boolean = stage match {
      case _: Triggered => true
      case _            => false
    }
  }
  implicit case object InProgress extends StageLike[Started] {
    override def matches[S <: Stage](stage: S): Boolean = stage match {
      case _: Started => true
      case _          => false
    }
  }
  implicit case object Done extends StageLike[Finished] {
    override def matches[S <: Stage](stage: S): Boolean = stage match {
      case _: Finished => true
      case _           => false
    }
  }

  sealed trait Outcome
  case class Success(result: Any) extends Outcome
  case class Failed(cause: ExecutionFailedCause) extends Outcome
  case object TimedOut extends Outcome

}

case class Execution private (id: ExecutionId, stages: List[Execution.Stage] = Nil) extends Serializable {
  import Execution._

  def executionId = id

  def scheduleId: ScheduleId = executionId._1

  def stage: Stage = stages.head

  def lastStatusChange: ZonedDateTime = stage.when

  def <<(newStage: Stage): Execution = {
    if (stages.nonEmpty && stage > newStage) {
      throw new IllegalArgumentException(
        s"Can't move the execution status to an earlier stage. executionId=$executionId, currentStage=$stage, newStage=$newStage"
      )
    }
    copy(stages = newStage :: stages)
  }

  def |>>[T <: Stage](stageLike: StageLike[T]): Option[T] = stageLike.grab(this)

  def is[T <: Stage](stageLike: StageLike[T]): Boolean = stageLike.currentIn(this)
  
  def outcome: Option[Outcome] = stage match {
    case Finished(_, _, outcome) => Some(outcome)
    case _                       => None
  }

}
