package io.chronos.scheduler.internal

import io.chronos.JobSchedule
import io.chronos.id._
import org.apache.ignite.Ignite

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by aalonsodominguez on 26/07/15.
 */
trait DistributedExecutionCache {
  protected implicit val ignite: Ignite

  private val traversing = ignite.atomicReference("traversingOverdue", false, true)

  private val scheduleCounter = ignite.atomicSequence("scheduleCounter", 0, true)
  private val scheduleMap = ignite.getOrCreateCache[ScheduleId, JobSchedule]("scheduleMap")
  private val scheduleByJob = ignite.getOrCreateCache[JobId, ScheduleId]("scheduleByHJob")

  def scheduleById(scheduleId: ScheduleId)(implicit ec: ExecutionContext): Future[Option[JobSchedule]] =
    Future { Option(scheduleMap.get(scheduleId)) }

}
