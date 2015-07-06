package io.chronos.scheduler

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.chronos.scheduler.servant.Frontend
import io.chronos.scheduler.worker.Work

import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object WorkProducer {
  case object Tick
}

class WorkProducer(endpoint: ActorRef) extends Actor with ActorLogging {
  import WorkProducer._
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
      val work = Work(nextWorkId, n)
      endpoint ! work
      context.become(waitAccepted(work), discardOld = false)
  }

  def waitAccepted(work: Work): Receive = {
    case Frontend.Accepted =>
      context.unbecome()
      scheduler.scheduleOnce(rnd.nextInt(3, 10).seconds, self, Tick)
    case Frontend.Rejected =>
      log.info("Work not accepted, retry after a while")
      scheduler.scheduleOnce(3.seconds, endpoint, Tick)
  }
}
