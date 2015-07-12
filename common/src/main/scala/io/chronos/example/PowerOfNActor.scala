package io.chronos.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.chronos.Facade
import io.chronos.protocol.SchedulerProtocol

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

  var n = 0

  override def preStart(): Unit =
    scheduler.scheduleOnce(5.seconds, self, Tick)

  override def postRestart(reason: Throwable): Unit = ()

  def receive = start

  def start: Receive = {
    case Tick =>
      receptor ! SchedulerProtocol.PublishJob(PowerOfNJobSpec)
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      context.become(produce)

  }

  def produce: Receive = {
    case Tick =>
      n += 1
      log.info("Produced work: {}", n)
      val jobDef = PowerOfNJobDef(n)
      receptor ! SchedulerProtocol.ScheduleJob(jobDef)
      context.become(waitAccepted, discardOld = false)
  }

  def waitAccepted: Receive = {
    case Facade.JobAccepted =>
      if (n < 10) {
        scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
      }
      context.unbecome()

    case Facade.JobRejected =>
      log.info("Work not accepted, retry after a while")
      scheduler.scheduleOnce(3.seconds, receptor, Tick)
      context.unbecome()
  }
}
