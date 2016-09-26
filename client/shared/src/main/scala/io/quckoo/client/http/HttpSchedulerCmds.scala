package io.quckoo.client.http

import io.quckoo.{ExecutionPlan, TaskExecution}
import io.quckoo.client.core._
import io.quckoo.id._
import io.quckoo.fault._
import io.quckoo.protocol.scheduler.{ExecutionPlanCancelled, ExecutionPlanStarted, ScheduleJob}
import io.quckoo.serialization.json._

import scalaz.\/

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait HttpSchedulerCmds extends HttpMarshalling with SchedulerCmds[HttpProtocol] {
  import CmdMarshalling.Auth

  private[this] def planUrl(cmd: Command[PlanId]): String =
    s"$ExecutionPlansURI/${cmd.payload}"
  private[this] def executionUrl(cmd: Command[TaskId]): String =
    s"$TaskExecutionsURI/${cmd.payload}"

  implicit lazy val scheduleJobCmd: ScheduleJobCmd = new Auth[HttpProtocol, ScheduleJob, JobNotFound \/ ExecutionPlanStarted] {
    override val marshall = marshallToJson[ScheduleJobCmd](HttpMethod.Put, _ => ExecutionPlansURI)
    override val unmarshall = unmarshalEither[JobId, ExecutionPlanStarted].map(_.leftMap(JobNotFound))
  }

  implicit lazy val getPlansCmd: GetPlansCmd = new Auth[HttpProtocol, Unit, Map[PlanId, ExecutionPlan]] {
    override val marshall = marshallEmpty[GetPlansCmd](HttpMethod.Get, _ => ExecutionPlansURI)
    override val unmarshall = unmarshallFromJson[GetPlansCmd]
  }

  implicit lazy val getPlanCmd: GetPlanCmd = new Auth[HttpProtocol, PlanId, Option[ExecutionPlan]] {
    override val marshall = marshallEmpty[GetPlanCmd](HttpMethod.Get, planUrl)
    override val unmarshall = unmarshalOption[ExecutionPlan]
  }

  implicit lazy val getExecutionsCmd: GetExecutionsCmd = new Auth[HttpProtocol, Unit, Map[TaskId, TaskExecution]] {
    override val marshall = marshallEmpty[GetExecutionsCmd](HttpMethod.Get, _ => TaskExecutionsURI)
    override val unmarshall = unmarshallFromJson[GetExecutionsCmd]
  }

  implicit lazy val getExecutionCmd: GetExecutionCmd = new Auth[HttpProtocol, TaskId, Option[TaskExecution]] {
    override val marshall = marshallEmpty[GetExecutionCmd](HttpMethod.Get, executionUrl)
    override val unmarshall = unmarshalOption[TaskExecution]
  }

  implicit lazy val cancelPlanCmd: CancelPlanCmd = new Auth[HttpProtocol, PlanId, ExecutionPlanNotFound \/ ExecutionPlanCancelled] {
    override val marshall = marshallEmpty[CancelPlanCmd](HttpMethod.Delete, planUrl)
    override val unmarshall = unmarshalEither[PlanId, ExecutionPlanCancelled].map(_.leftMap(ExecutionPlanNotFound))
  }
}
