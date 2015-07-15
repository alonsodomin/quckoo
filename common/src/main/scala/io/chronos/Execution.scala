package io.chronos

import java.time.{Clock, ZonedDateTime}

import io.chronos.id._

/**
 * Created by aalonsodominguez on 11/07/15.
 */
object Execution {

  def apply(executionId: ExecutionId)(implicit c: Clock): Execution = {
    new Execution(executionId) >> Scheduled(ZonedDateTime.now(c))
  }
  
  sealed abstract class Stage(val ordinal: Int) extends Ordered[Stage] with Serializable {
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
  case class Failed(cause: Throwable) extends Outcome with Status
  case object TimedOut extends Outcome with Status

  type StageType = Class[_ <: Stage]
  type StatusType = Class[_ <: Status]

  val ScheduledStage = classOf[Scheduled]
  val TriggeredStage = classOf[Triggered]
  val StartedStage = classOf[Started]
  val FinishedStage = classOf[Finished]

  implicit def is(status: StatusType): Boolean =
    implicitly[Execution].is(status)
  implicit def at(stage: StageType, between: (ZonedDateTime, ZonedDateTime)): Boolean =
    implicitly[Execution].at(stage, between)

  implicit def >> (stage: Stage): Execution = implicitly[Execution].>>(stage)
  
  def compareByDate(stage: StageType): Ordering[Execution] = new Ordering[Execution] {
    override def compare(x: Execution, y: Execution): Int = {
      val comparison = for (
        xStage <- x(stage);
        yStage <- y(stage)
      ) yield xStage.when.compareTo(yStage.when)
      comparison getOrElse 0
    }
  }

}

case class Execution(id: ExecutionId, stages: List[Execution.Stage] = Nil) extends Serializable {
  import Execution._

  def apply(stageType: StageType): Option[Stage] = stages.find(st => stageType.isInstance(st))

  def executionId = id

  def scheduleId: ScheduleId = executionId._1

  def stage: Stage = stages.head

  def lastStatusChange: ZonedDateTime = stage.when

  def >> (newStage: Stage): Execution = {
    require(newStage > stage)
    copy(stages = newStage :: stages)
  }

  def is(status: StatusType): Boolean = outcome match {
    case Some(o) => status.isInstance(o) || status.isInstance(stage)
    case None    => status.isInstance(stage)
  }

  def at(stage: StageType, between: (ZonedDateTime, ZonedDateTime)): Boolean = {
    def stageFilter(st: Stage): Boolean = {
      stage.isInstance(st) && (
        (st.when.isEqual(between._1) || st.when.isAfter(between._1)) &&
          (st.when.isBefore(between._2) || st.when.isEqual(between._2))
      )
    }
    stages.count(stageFilter) > 0
  }

  def outcome: Option[Outcome] = stage match {
    case Finished(_, _, outcome) => Some(outcome)
    case _                       => None
  }

}
