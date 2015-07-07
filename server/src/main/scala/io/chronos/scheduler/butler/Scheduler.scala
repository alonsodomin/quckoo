package io.chronos.scheduler.butler

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.hazelcast.core.Hazelcast
import com.hazelcast.query.PagingPredicate
import io.chronos.scheduler.JobDefinition
import io.chronos.scheduler.id._
import io.chronos.scheduler.worker.Work

import scala.collection.JavaConversions._
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 07/07/15.
 */
object Scheduler {

  val defaultHeartbeatInterval = 100.millis

  def props(heartbeatInterval: FiniteDuration): Props = Props(classOf[Scheduler], heartbeatInterval)

  private case object Heartbeat

}

class Scheduler(director: ActorRef, heartbeatInterval: FiniteDuration) extends Actor with ActorLogging {
  import Scheduler._

  private val hazelcastInstance = Hazelcast.newHazelcastInstance()

  private val executionCounter = hazelcastInstance.getAtomicLong("executionCounter")
  private val jobDefinitions = hazelcastInstance.getMap[JobId, JobDefinition]("jobDefinitions")

  val heartbeatTask = context.system.scheduler.schedule(0.seconds, heartbeatInterval, self, Heartbeat)

  override def postStop(): Unit = heartbeatTask.cancel()

  def receive = {
    case Heartbeat =>
      val predicate = new PagingPredicate()

      jobDefinitions.values(predicate).foreach { jobDef =>
        jobDefinitions.remove(jobDef.jobId)
        val workId: WorkId = (jobDef.jobId, executionCounter.incrementAndGet())
        director ! Work(workId)
      }
  }

}
