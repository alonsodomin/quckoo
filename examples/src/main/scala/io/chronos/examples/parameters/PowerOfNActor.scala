package io.chronos.examples.parameters

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.chronos._
import io.chronos.id.ModuleId
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}

import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object PowerOfNActor {

  def props(client: ActorRef): Props = Props(classOf[PowerOfNActor], client)

  private case object Tick
}

class PowerOfNActor(client: ActorRef) extends Actor with ActorLogging {
  import PowerOfNActor._
  import RegistryProtocol._
  import SchedulerProtocol._
  import context.dispatcher

  def scheduler = context.system.scheduler
  def rnd = ThreadLocalRandom.current

  val jobSpec = JobSpec(id = UUID.randomUUID(),
    displayName = "Power Of N",
    moduleId = ModuleId("io.chronos", "examples_2.11", "0.1.0-SNAPSHOT"),
    jobClass = classOf[PowerOfNJob].getName
  )
  var n = 0

  override def preStart(): Unit =
    scheduler.scheduleOnce(5.seconds, self, Tick)

  override def postRestart(reason: Throwable): Unit = ()

  def receive = start

  def start: Receive = {
    case Tick =>
      client ! RegisterJob(jobSpec)

    case JobAccepted(jobId, _) if jobId == jobSpec.id =>
      log.info("JobSpec has been registered. Moving on to produce job schedules.")
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      context.become(produce)

    case JobRejected(cause) =>
      cause match {
        case Left(resolutionFailed) =>
          log.error("Resolution of job spec failed. unresolvedDependencies={}", resolutionFailed.unresolvedDependencies.mkString(", "))

        case Right(thrown) =>
          log.error("Registration of job spec failed due to an exception. Retrying...")
          scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      }
  }

  def produce: Receive = {
    case Tick =>
      n += 1
      log.info("Produced work: {}", n)
      client ! ScheduleJob(jobSpec.id, Map("n" -> n), jobTrigger)
      context.become(waitAccepted, discardOld = false)
  }

  def waitAccepted: Receive = {
    case JobScheduled(jobId, planId) if jobId == jobSpec.id =>
      log.info("Job schedule has been accepted by the cluster. executionPlanId={}", planId)
      if (n < 25) {
        scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      }
      context.unbecome()

    case JobNotEnabled(jobId) if jobId == jobSpec.id =>
      log.error("Job scheduling has failed because the job hasn't been registered in the first place. jobId={}", jobId)

    case JobFailedToSchedule(jobId, cause) if jobId == jobSpec.id =>
      log.error("Job scheduling has thrown an error. Will retry after a while. message={}", cause.getMessage)
      scheduler.scheduleOnce(3.seconds, self, Tick)
      context.unbecome()
  }

  private def jobTrigger: Trigger = rnd.nextInt(0, 3) match {
    case 1 => // After random delay
      val delay = rnd.nextInt(1, 30)
      Trigger.After(delay seconds)
    case 2 => // Every random seconds
      val freq = rnd.nextInt(5, 10)
      val delay = rnd.nextInt(5, 30)
      Trigger.Every(freq seconds, Option(delay seconds))
    case _ => // Immediate
      Trigger.Immediate
  }

}
