/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.examples.parameters

import java.util.concurrent.ThreadLocalRandom

import akka.actor._

import io.quckoo._
import io.quckoo.fault.JobNotEnabled
import io.quckoo.id.{ArtifactId, JobId}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._

import scala.concurrent.duration._

/**
  * Created by aalonsodominguez on 05/07/15.
  */
object PowerOfNActor {

  def props(client: ActorRef): Props = Props(classOf[PowerOfNActor], client)

  private case object Tick
}

class PowerOfNActor(client: ActorRef) extends Actor with ActorLogging {
  import PowerOfNActor._
  import context.dispatcher

  def scheduler = context.system.scheduler
  def rnd       = ThreadLocalRandom.current

  val jobSpec = JobSpec(
    displayName = "Power Of N",
    artifactId = ArtifactId("io.quckoo", "quckoo-example-jobs_2.11", "0.1.0-SNAPSHOT"),
    jobClass = classOf[PowerOfNJob].getName
  )

  var n            = 0
  var jobId: JobId = _

  private[this] var scheduleTask: Option[Cancellable] = None

  override def preStart(): Unit =
    scheduleTask = Some(scheduler.scheduleOnce(5.seconds, self, Tick))

  override def postRestart(reason: Throwable): Unit = ()

  override def postStop(): Unit = {
    scheduleTask.foreach(_.cancel())
    scheduleTask = None
  }

  def receive: Receive = start

  def start: Receive = {
    case Tick =>
      client ! RegisterJob(jobSpec)
      context.setReceiveTimeout(60 seconds)

    case JobAccepted(id, _) =>
      log.info("JobSpec has been registered with id {}. Moving on to produce job schedules.", id)
      jobId = id
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      context.become(produce)

    case JobRejected(_, cause) =>
      log.error("Resolution of job spec failed. cause={}", cause.toString)
      context.setReceiveTimeout(Duration.Undefined)

    case ReceiveTimeout =>
      log.warning(
        "Timeout waiting for a response when registering a job specification. Retrying...")
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
  }

  def produce: Receive = {
    case Tick =>
      n += 1
      log.info("Produced work: {}", n)
      //client ! ScheduleJob(jobId, Map("n" -> n), jobTrigger)
      client ! ScheduleJob(jobId, jobTrigger)
      context.become(waitAccepted, discardOld = false)
  }

  def waitAccepted: Receive = {
    case TaskScheduled(id, planId, taskId, _) if jobId == id =>
      log.info("Job schedule has been accepted by the cluster. executionPlanId={}", planId)
      if (n < 25) {
        scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      }
      context.unbecome()

    case JobNotEnabled(id) if jobId == id =>
      log.error(
        "Job scheduling has failed because the job hasn't been registered in the first place. jobId={}",
        jobId)

    case JobFailedToSchedule(id, cause) if jobId == id =>
      log.error(
        "Job scheduling has thrown an error. Will retry after a while. message={}",
        cause.toString)
      scheduler.scheduleOnce(3.seconds, self, Tick)
      context.unbecome()
  }

  private def jobTrigger: Trigger = rnd.nextInt(0, 3) match {
    case 1 => // After random delay
      val delay = rnd.nextInt(1, 30)
      Trigger.After(delay seconds)
    case 2 => // Every random seconds
      val freq  = rnd.nextInt(5, 10)
      val delay = rnd.nextInt(5, 30)
      Trigger.Every(freq seconds, Option(delay seconds))
    case _ => // Immediate
      Trigger.Immediate
  }

}
