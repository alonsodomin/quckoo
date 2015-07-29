package io.chronos.scheduler

import akka.actor.{Actor, ActorRef}
import akka.pattern._
import akka.util.Timeout
import io.chronos.id._
import io.chronos.protocol.WorkerProtocol.WorkReady

import scala.concurrent.duration._

/**
 * Created by domingueza on 29/07/15.
 */
object WorkDistributorActor {
  private case object CleanupTick
}

class WorkDistributorActor(maxWorkTimeout: FiniteDuration, queueRef: ActorRef) extends Actor {
  import WorkDistributorActor._

  private var workers = Map.empty[WorkerId, WorkerState]

  private val cleanupTask = context.system.scheduler.schedule(maxWorkTimeout / 2, maxWorkTimeout / 2, self, CleanupTick)

  override def postStop(): Unit = cleanupTask.cancel()

  def receive = {
    case CleanupTick =>
  }

  private def notifyWorkers(): Unit = {
    implicit val timeout = Timeout(5 seconds)
    (queueRef ? ExecutionQueueActor.PendingExecutionsCount).mapTo[Int].foreach { count =>
      if (count > 0) workers.foreach {
        case (_, WorkerState(ref, WorkerState.Idle)) => ref ! WorkReady
        case _ => // busy
      }
    }
  }

  private def changeWorkerToIdle(workerId: WorkerId, executionId: ExecutionId): Unit = workers.get(workerId) match {
    case Some(workerState @ WorkerState(_, WorkerState.Busy(`executionId`, _))) =>
      workers += (workerId -> workerState.copy(status = WorkerState.Idle))
    case _ => // might happen after standby recovery, worker state is not persisted
  }

}
