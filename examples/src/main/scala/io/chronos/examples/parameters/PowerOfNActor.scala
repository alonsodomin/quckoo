package io.chronos.examples.parameters

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.chronos._
import io.chronos.examples.FacadeActor
import io.chronos.id.JobModuleId
import io.chronos.protocol.SchedulerProtocol

import scala.collection.immutable.HashMap
import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object PowerOfNActor {

  def props(receptor: ActorRef): Props = Props(classOf[PowerOfNActor], receptor)

  case object Tick
}

class PowerOfNActor(receptor: ActorRef) extends Actor with ActorLogging {
  import PowerOfNActor._
  import context.dispatcher

  def scheduler = context.system.scheduler
  def rnd = ThreadLocalRandom.current

  val jobSpec = JobSpec(id = UUID.randomUUID(),
    displayName = "Power Of N",
    moduleId = JobModuleId("io.chronos", "examples_2.11", "0.1.0-SNAPSHOT"),
    jobClass = classOf[PowerOfNJob].getName
  )
  var n = 0

  override def preStart(): Unit =
    scheduler.scheduleOnce(5.seconds, self, Tick)

  override def postRestart(reason: Throwable): Unit = ()

  def receive = start

  def start: Receive = {
    case Tick =>
      receptor ! SchedulerProtocol.RegisterJob(jobSpec)

    case SchedulerProtocol.RegisterJobAck =>
      log.info("JobSpec has been registered. Moving on to produce job schedules.")
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      context.become(produce)
  }

  def produce: Receive = {
    case Tick =>
      n += 1
      log.info("Produced work: {}", n)
      val delay  = rnd.nextInt(1, 30)
      val jobDef = JobSchedule(jobSpec.id, HashMap("n" -> n), Trigger.After(delay.seconds))
      receptor ! SchedulerProtocol.ScheduleJob(jobDef)
      context.become(waitAccepted, discardOld = false)
  }

  def waitAccepted: Receive = {
    case FacadeActor.JobAccepted =>
      if (n < 25) {
        scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      }
      context.unbecome()

    case FacadeActor.JobRejected =>
      log.info("Work not accepted, retry after a while")
      scheduler.scheduleOnce(3.seconds, self, Tick)
      context.unbecome()
  }
}
