package io.chronos.scheduler

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.chronos.scheduler.receptor.ReceptorActor
import io.chronos.scheduler.worker.Work

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

  def job: String = {
    val n2 = n * n
    s"$n * $n = $n2"
  }

  override def preStart(): Unit =
    scheduler.scheduleOnce(5.seconds, self, Tick)

  override def postRestart(reason: Throwable): Unit = ()

  def receive = {
    case Tick =>
      n += 1
      log.info("Produced work: {}", n)
      val work = Work(nextWorkId, n)
      receptor ! work
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
