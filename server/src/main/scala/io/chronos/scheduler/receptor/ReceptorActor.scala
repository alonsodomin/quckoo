package io.chronos.scheduler.receptor

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.ClusterSingletonProxy
import akka.pattern._
import akka.util.Timeout
import com.hazelcast.client.HazelcastClient
import io.chronos.scheduler.JobDefinition
import io.chronos.scheduler.butler.Butler
import io.chronos.scheduler.jobstore.HazelcastJobStore

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

object ReceptorActor {
  sealed trait ReceptorMessage
  case class RegisterJob(job: JobDefinition) extends ReceptorMessage

  sealed trait ReceptorResponse
  case object Accepted extends ReceptorResponse
  case object Rejected extends ReceptorResponse
}

class ReceptorActor extends Actor with ActorLogging {
  import ReceptorActor._
  import context.dispatcher

  var butlerProxy = context.actorOf(ClusterSingletonProxy.props(
    singletonPath = Butler.Path,
    role = Some("backend")
  ), name = "butlerProxy")

  private val hazelcastClient = HazelcastClient.newHazelcastClient()
  private val jobStore = new HazelcastJobStore(hazelcastClient)

  def nextWorkId = UUID.randomUUID().toString

  def receive = {
    case RegisterJob(jobDef: JobDefinition) =>
      log.info("Registering job {}", jobDef.jobId)
      jobStore.push(jobDef)
      Accepted

    case work =>
      implicit val timeout = Timeout(5.seconds)
      (butlerProxy ? work) map {
        case Butler.Ack(_) => Accepted
      } recover {
        case _ => Rejected
      } pipeTo sender()
  }

}
