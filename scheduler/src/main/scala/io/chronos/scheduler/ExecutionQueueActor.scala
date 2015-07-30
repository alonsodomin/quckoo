package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import akka.pattern._
import akka.util.Timeout
import io.chronos._
import io.chronos.id._
import io.chronos.protocol.SchedulerProtocol.{ExecutionEvent, WorkerRegistered}
import io.chronos.protocol.WorkerProtocol.{RegisterWorker, RequestWork, WorkReady}
import org.apache.ignite.Ignite

import scala.concurrent.duration._

/**
 * Created by domingueza on 29/07/15.
 */
object ExecutionQueueActor {

  case class Enqueue(execution: Execution)

  private case object CleanupTick

}

class ExecutionQueueActor(ignite: Ignite, queueCapacity: Int, maxWorkTimeout: FiniteDuration)(implicit clock: Clock)
  extends Actor with ActorLogging {

  import ExecutionQueueActor._
  import context.dispatcher
  import io.chronos.protocol._

  private val mediator = DistributedPubSubExtension(context.system).mediator
  private val registry = context.system.actorSelection(context.system / "registry")
  private val executionPlan = context.system.actorSelection(context.system / "execution" / "plan")

  private val executionQueue = ignite.queue[ExecutionId]("executionQueue", queueCapacity, null)

  private var workers = Map.empty[WorkerId, WorkerState]

  private val cleanupTask = context.system.scheduler.schedule(maxWorkTimeout / 2, maxWorkTimeout / 2, self, CleanupTick)

  override def postStop(): Unit = cleanupTask.cancel()

  def receive = {
    case Enqueue(execution) =>
      executionQueue.put(execution.executionId)

    case RegisterWorker(workerId) =>
      if (workers.contains(workerId)) {
        workers += (workerId -> workers(workerId).copy(ref = sender()))
      } else {
        workers += (workerId -> WorkerState(sender(), status = WorkerState.Idle))
        log.info("Worker registered: {}", workerId)
        if (!executionQueue.isEmpty) {
          sender() ! WorkReady
        }
        mediator ! DistributedPubSubMediator.Publish(topic.Workers, WorkerRegistered(workerId))
      }

    case RequestWork(workerId) if !executionQueue.isEmpty =>
      workers.get(workerId) match {
        case Some(workerState@WorkerState(_, WorkerState.Idle)) =>
          val executionId = executionQueue.take()

          implicit val timeout = Timeout(5 seconds)
          val futureJobSpec = (registry ? GetJobSpec(executionId._1._1)).mapTo[Option[JobSpec]]
          val futureSchedule = (executionPlan ? GetSchedule(executionId._1)).mapTo[Option[JobSchedule]]
          futureJobSpec.zip(futureSchedule).map(x => (x._1, x._2)).onSuccess {
            case (Some(jobSpec), Some(schedule)) =>
              def workTimeout: Deadline = Deadline.now + schedule.timeout.getOrElse(maxWorkTimeout)

              log.info("Delivering execution to worker. executionId={}, workerId={}", executionId, workerId)
              sender ! Work(executionId, schedule.params, jobSpec.moduleId, jobSpec.jobClass)

              workers += (workerId -> workerState.copy(status = WorkerState.Busy(executionId, workTimeout)))

              val executionStage = Execution.Started(ZonedDateTime.now(clock), workerId)
              // TODO think of a different message to send in here
              executionPlan ! ExecutionEvent(executionId, executionStage)
          }
      }

    case CleanupTick =>
  }

  private def notifyWorkers(): Unit = {
    if (!executionQueue.isEmpty) {
      workers.foreach {
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
