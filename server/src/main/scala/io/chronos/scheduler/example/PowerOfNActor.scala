package io.chronos.scheduler.example

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.chronos.scheduler.receptor.ReceptorActor

import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object PowerOfNActor {
  case object Tick
}

class PowerOfNActor(receptor: ActorRef) extends Actor with ActorLogging {
  import PowerOfNActor._
  import context.dispatcher

  def scheduler = context.system.scheduler
  def rnd = ThreadLocalRandom.current

  def nextWorkId = UUID.randomUUID().toString

  var n = 0

  override def preStart(): Unit =
    scheduler.scheduleOnce(5.seconds, self, Tick)

  override def postRestart(reason: Throwable): Unit = ()

  def receive = {
    case Tick =>
      n += 1
      log.info("Produced work: {}", n)
      val jobDef = PowerOfNJobDef
      receptor ! ReceptorActor.RegisterJob(jobDef)
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
