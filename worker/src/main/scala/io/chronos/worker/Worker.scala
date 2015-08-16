package io.chronos.worker

import java.util.UUID

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.cluster.client.ClusterClient.SendToAll
import io.chronos.cluster.{Work, WorkerProtocol}
import io.chronos.path

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object Worker {

  def props(clusterClient: ActorRef, jobExecutorProps: Props, registerInterval: FiniteDuration = 10.seconds): Props =
    Props(classOf[Worker], clusterClient, jobExecutorProps, registerInterval)

  case class WorkerDone(executionId: String, result: Any)

}

class Worker(clusterClient: ActorRef, jobExecutorProps: Props, registerInterval: FiniteDuration) extends Actor with ActorLogging {
  import WorkerProtocol._
  import context.dispatcher

  val workerId = UUID.randomUUID()
  
  val registerTask = context.system.scheduler.schedule(
    0.seconds, registerInterval, clusterClient,
    SendToAll(path.Scheduler, RegisterWorker(workerId))
  )

  val jobExecutor = context.watch(context.actorOf(jobExecutorProps, "executor"))
  
  private var currentExecutionId: Option[UUID] = None

  def executionId: UUID = currentExecutionId match {
    case Some(id) => id
    case None     => throw new IllegalStateException("Not working")
  }

  override def postStop(): Unit = registerTask.cancel()

  def receive = idle

  def idle: Receive = {
    case WorkReady =>
      log.info("Requesting work to master.")
      sendToMaster(RequestWork(workerId))

    case work: Work =>
      log.info("Received work for execution {}", work.executionId)
      currentExecutionId = Some(work.executionId)
      jobExecutor ! JobExecutor.Execute(work)
      context.become(working)
  }

  def working: Receive = {
    case JobExecutor.Completed(executionId, result) =>
      log.info("Work is complete. Result {}.", result)
      sendToMaster(WorkDone(workerId, executionId, result))
      context.setReceiveTimeout(5 seconds)
      context.become(waitForWorkDoneAck(result))

    case JobExecutor.Failed(executionId, reason) =>
      sendToMaster(WorkFailed(workerId, executionId, reason))
      context.setReceiveTimeout(5 seconds)
      context.become(idle)

    case _: Work =>
      log.info("Yikes. Master told me to do work, while I'm working.")
  }

  def waitForWorkDoneAck(result: Any): Receive = {
    case WorkDoneAck(id) if id == executionId =>
      sendToMaster(RequestWork(workerId))
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case ReceiveTimeout =>
      log.info("No ack from master, retrying")
      sendToMaster(WorkDone(workerId, executionId, result))
  }

  private def sendToMaster(msg: Any): Unit = {
    clusterClient ! SendToAll(path.Scheduler, msg)
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException => Stop
    case cause: Exception =>
      currentExecutionId.foreach {
        executionId => sendToMaster(WorkFailed(workerId, executionId, Right(cause)))
      }
      context.become(idle)
      Restart
  }

  override def unhandled(message: Any): Unit = message match {
    case Terminated(`jobExecutor`) => context.stop(self)
    case WorkReady =>
    case _ => super.unhandled(message)
  }

}