package io.chronos.scheduler.internal

import java.time.Clock

import io.chronos.JobSchedule
import io.chronos.id._
import org.apache.ignite.Ignite
import org.apache.ignite.cache.query.ScanQuery
import org.apache.ignite.lang.IgniteBiPredicate
import resource._

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by aalonsodominguez on 26/07/15.
 */
trait DistributedScheduleCache {
  protected implicit val ignite: Ignite

  private val scheduleCounter = ignite.atomicSequence("scheduleCounter", 0, true)
  protected val scheduleMap = ignite.getOrCreateCache[ScheduleId, JobSchedule]("scheduleMap")
  
  def scheduleById(scheduleId: ScheduleId)(implicit ec: ExecutionContext): Future[Option[JobSchedule]] =
    Future { Option(scheduleMap.get(scheduleId)) }

  def schedulesByJob(jobId: JobId)(implicit ec: ExecutionContext): Future[Seq[ScheduleId]] = Future {
    def iterate(it: Iterator[(ScheduleId, JobSchedule)], acc: Seq[ScheduleId]): Seq[ScheduleId] = {
      if (!it.hasNext) acc
      else iterate(it, acc :+ it.next()._1)
    }
    def filter(pair: (ScheduleId, JobSchedule)): Boolean = pair._1._1 == jobId
    iterate(scheduleMap.getAll(Set.empty).view.filter(filter).iterator, Vector())
  }

  protected def scanSchedules(filter: JobSchedule => Boolean, f: (ScheduleId, JobSchedule) => Unit): Unit = {
    val q = new ScanQuery[ScheduleId, JobSchedule](new IgniteBiPredicate[ScheduleId, JobSchedule] {
      override def apply(e1: ScheduleId, e2: JobSchedule): Boolean = filter(e2)
    })
    
    for { 
      cursor <- managed(scheduleMap.query(q))
      entry  <- cursor
    } f(entry.getKey, entry.getValue)
  }

  protected def newSchedule(jobSchedule: JobSchedule)(implicit clock: Clock, ec: ExecutionContext): Future[ScheduleId] =
    Future {
      val scheduleId = (jobSchedule.jobId, scheduleCounter.incrementAndGet())
      scheduleMap.put(scheduleId, jobSchedule)
      scheduleId
    }

}
