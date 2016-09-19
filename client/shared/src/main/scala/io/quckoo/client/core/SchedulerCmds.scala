package io.quckoo.client.core

import io.quckoo.{ExecutionPlan, TaskExecution}
import io.quckoo.id._
import io.quckoo.protocol.registry.JobNotFound
import io.quckoo.protocol.scheduler.{ExecutionPlanNotFound, ExecutionPlanStarted, ScheduleJob}

import scalaz.\/

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait SchedulerCmds[P <: Protocol] {
  import CmdMarshalling.Auth

  type ScheduleJobCmd   = Auth[P, ScheduleJob, JobNotFound \/ ExecutionPlanStarted]
  type GetPlansCmd      = Auth[P, Unit, Map[PlanId, ExecutionPlan]]
  type GetPlanCmd       = Auth[P, PlanId, Option[ExecutionPlan]]
  type GetExecutionsCmd = Auth[P, Unit, Map[TaskId, TaskExecution]]
  type GetExecutionCmd  = Auth[P, TaskId, Option[TaskExecution]]
  type CancelPlanCmd    = Auth[P, PlanId, ExecutionPlanNotFound \/ Unit]

  implicit def scheduleJobCmd: ScheduleJobCmd
  implicit def getPlansCmd: GetPlansCmd
  implicit def getPlanCmd: GetPlanCmd
  implicit def getExecutionsCmd: GetExecutionsCmd
  implicit def getExecutionCmd: GetExecutionCmd
  implicit def cancelPlanCmd: CancelPlanCmd
}
