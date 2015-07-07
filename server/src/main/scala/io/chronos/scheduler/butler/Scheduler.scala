package io.chronos.scheduler.butler

import java.time.Clock

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.contrib.pattern.ClusterSingletonProxy
import com.hazelcast.core.Hazelcast
import io.chronos.scheduler.JobDefinition
import io.chronos.scheduler.id._
import io.chronos.scheduler.jobstore.HazelcastJobStore

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 07/07/15.
 */
object Scheduler {

  val defaultHeartbeatInterval = 100.millis

  def props(clock: Clock, heartbeatInterval: FiniteDuration, jobBatchSize: Int): Props =
    Props(classOf[Scheduler], clock, heartbeatInterval, jobBatchSize)

  private case object Heartbeat

}

class Scheduler(clock: Clock, director: ActorRef, heartbeatInterval: FiniteDuration, jobBatchSize: Int) extends Actor with ActorLogging {
  import Scheduler._

  var butlerProxy = context.actorOf(ClusterSingletonProxy.props(
    singletonPath = Butler.Path,
    role = Some("backend")
  ), name = "butlerProxy")

  private val hazelcastInstance = Hazelcast.newHazelcastInstance()
  private val jobStore = new HazelcastJobStore(hazelcastInstance)

  private val pendingAck = hazelcastInstance.getMap[WorkId, JobDefinition]("jobPendingAck")

  val heartbeatTask = context.system.scheduler.schedule(0.seconds, heartbeatInterval, self, Heartbeat)

  override def postStop(): Unit = heartbeatTask.cancel()

  def receive = {
    case Heartbeat =>
      jobStore.pollOverdueJobs(clock, jobBatchSize) { jobDef =>
        val work = jobStore.createWork(jobDef)
        log.info("Dispatching job to master. workId={}", work.id)
        director ! work
        pendingAck.put(work.id, jobDef)
      }

    case Butler.Ack(workId) =>
      val jobDef = Option(pendingAck.remove(workId))
      jobDef match {
        case Some(_) =>
          log.debug("Work acknowledged by the master. workId={}", workId)
        case None =>
          log.warning("Received acknowledge of a non-enqueued work. workId={}", workId)
      }
  }

}
