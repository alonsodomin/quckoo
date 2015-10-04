package io.chronos.examples.parameters

import akka.actor._
import io.chronos._
import io.chronos.id.{JobId, ModuleId}
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

  val jobSpec = JobSpec(
    displayName = "Power Of N",
    moduleId = ModuleId("io.chronos", "example-jobs_2.11", "0.1.0-SNAPSHOT"),
    jobClass = classOf[PowerOfNJob].getName
  )

  var n = 0
  var jobId: JobId = _

  override def preStart(): Unit =
    scheduler.scheduleOnce(5.seconds, self, Tick)

  override def postRestart(reason: Throwable): Unit = ()

  def receive = start

  def start: Receive = {
    case Tick =>
      client ! RegisterJob(jobSpec)
      context.setReceiveTimeout(30 seconds)

    case JobAccepted(id, _) =>
      log.info("JobSpec has been registered with id {}. Moving on to produce job schedules.", id)
      jobId = id
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      context.become(produce)

    case JobRejected(_, cause) =>
      cause match {
        case Left(resolutionFailed) =>
          log.error("Resolution of job spec failed. unresolvedDependencies={}", resolutionFailed.unresolvedDependencies.mkString(", "))

        case Right(thrown) =>
          log.error("Registration of job spec failed due to an exception. Retrying...")
          scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      }
      context.setReceiveTimeout(Duration.Undefined)

    case ReceiveTimeout =>
      log.warning("Timeout waiting for a response when registering a job specification. Retrying...")
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
  }

  def produce: Receive = {
    case Tick =>
      n += 1
      log.info("Produced work: {}", n)
      client ! ScheduleJob(jobId, Map("n" -> n), jobTrigger)
      context.become(waitAccepted, discardOld = false)
  }

  def waitAccepted: Receive = {
    case JobScheduled(id, planId) if jobId == id =>
      log.info("Job schedule has been accepted by the cluster. executionPlanId={}", planId)
      if (n < 25) {
        scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      }
      context.unbecome()

    case JobNotEnabled(id) if jobId == id =>
      log.error("Job scheduling has failed because the job hasn't been registered in the first place. jobId={}", jobId)

    case JobFailedToSchedule(id, cause) if jobId == id =>
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
