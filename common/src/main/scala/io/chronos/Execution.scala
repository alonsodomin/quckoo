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

  sealed trait Status

  sealed trait Waiting extends Stage with Status
  sealed trait InProgress extends Stage with Status
  sealed trait Complete extends Stage with Status

  val Pending = classOf[Waiting]
  val Running = classOf[InProgress]
  val Done = classOf[Complete]

  case class Scheduled(when: ZonedDateTime) extends Stage(1) with Waiting
  case class Triggered(when: ZonedDateTime) extends Stage(2) with Waiting with InProgress
  case class Started(when: ZonedDateTime, where: WorkerId) extends Stage(3) with InProgress
  case class Finished(when: ZonedDateTime, where: WorkerId, outcome: Outcome) extends Stage(4) with Complete

  sealed trait Outcome
  case class Success(result: Any) extends Outcome with Status
  case class Failed(cause: ExecutionFailedCause) extends Outcome with Status
  case object TimedOut extends Outcome with Status

  type StageType = Class[_ <: Stage]
  type StatusType = Class[_ <: Status]

  val ScheduledStage = classOf[Scheduled]
  val TriggeredStage = classOf[Triggered]
  val StartedStage = classOf[Started]
  val FinishedStage = classOf[Finished]

}

case class Execution private (id: ExecutionId, stages: List[Execution.Stage] = Nil) extends Serializable {
  import Execution._

  def apply(stageType: StageType): Option[Stage] = stages.find(st => stageType.isInstance(st))

  def executionId = id

  def scheduleId: ScheduleId = executionId._1

  def stage: Stage = stages.head

  def lastStatusChange: ZonedDateTime = stage.when

  def << (newStage: Stage): Execution = {
    if (stages.nonEmpty && stage > newStage) {
      throw new IllegalArgumentException(
        s"Can't move the execution status to an earlier stage. executionId=$executionId, currentStage=$stage, newStage=$newStage"
      )
    }
    copy(stages = newStage :: stages)
  }

  def is(status: StatusType): Boolean = outcome match {
    case Some(o) => status.isInstance(o) || status.isInstance(stage)
    case None    => status.isInstance(stage)
  }

  def at(stage: StageType, between: (ZonedDateTime, ZonedDateTime)): Boolean = {
    def stageFilter(st: Stage): Boolean = {
      val (startDate, endDate) = between
      stage.isInstance(st) && (
        (st.when.isEqual(startDate) || st.when.isAfter(startDate)) &&
          (st.when.isBefore(endDate) || st.when.isEqual(endDate))
      )
    }
    stages.count(stageFilter) > 0
  }

  def outcome: Option[Outcome] = stage match {
    case Finished(_, _, outcome) => Some(outcome)
    case _                       => None
  }

}
