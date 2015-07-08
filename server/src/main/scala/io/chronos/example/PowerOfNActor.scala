package io.chronos.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.chronos.protocol.SchedulerProtocol
import io.chronos.receptor.ReceptorActor

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

  def receive = {
    case Tick =>
      n += 1
      log.info("Produced work: {}", n)
      val jobDef = PowerOfNJobDef(n)
      receptor ! SchedulerProtocol.ScheduleJob(jobDef)
      context.become(waitAccepted, discardOld = false)
  }

  def waitAccepted: Receive = {
    case ReceptorActor.Accepted =>
      context.unbecome()
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
    case ReceptorActor.Rejected =>
      log.info("Work not accepted, retry after a while")
      scheduler.scheduleOnce(3.seconds, receptor, Tick)
  }
}
