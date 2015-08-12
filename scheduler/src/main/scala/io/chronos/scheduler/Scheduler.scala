package io.chronos.scheduler

import java.time.ZonedDateTime

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern._
import akka.persistence.PersistentActor
import akka.util.Timeout
import io.chronos.id.{ExecutionId, ScheduleId}
import io.chronos.protocol.{GetJob, ScheduleJob2}
import io.chronos.{JobSpec, Trigger}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Success

/**
 * Created by aalonsodominguez on 11/08/15.
 */
object Scheduler {

  case class ScheduleCreated(scheduleId: ScheduleId, jobSpec: JobSpec, trigger: Trigger, timeout: Option[FiniteDuration] = None)

  case class ExecutionScheduled(executionId: ExecutionId, due: ZonedDateTime) extends Ordered[ExecutionScheduled] {
    override def compare(that: ExecutionScheduled): Int = due compareTo that.due
  }

  private case object Heartbeat

}

class Scheduler(jobRegistry: ActorRef) extends PersistentActor with ActorLogging {
  import Scheduler._

  private var scheduleCount: Long = 0
  private var executionCount: Long = 0

  private val scheduleQueue = mutable.PriorityQueue.empty[ExecutionScheduled]

  override def persistenceId: String = ???

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = {
    case schedule: ScheduleJob2 =>
      implicit val timeout = Timeout(5 seconds)
      (jobRegistry ? GetJob(schedule.jobId)).mapTo[JobSpec] onComplete {
        case Success(jobSpec) =>
          scheduleCount += 1
          persist(ScheduleCreated((schedule.jobId, scheduleCount), jobSpec, schedule.trigger, schedule.timeout)) { event =>
            executionCount += 1

          }
      }
  }

}
