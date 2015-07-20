package io.chronos.worker

import java.util.UUID

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.contrib.pattern.ClusterClient.SendToAll
import io.chronos.id.ExecutionId
import io.chronos.protocol.WorkerProtocol
import io.chronos.{Work, path}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object Worker {

  def props(clusterClient: ActorRef, jobExecutorProps: Props, registerInteval: FiniteDuration = 10.seconds): Props =
    Props(classOf[Worker], clusterClient, jobExecutorProps, registerInteval)

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
  
  private var currentExecutionId: Option[ExecutionId] = None

  def executionId: ExecutionId = currentExecutionId match {
    case Some(id) => id
    case None     => throw new IllegalStateException("Not working")
  }

  override def postStop(): Unit = registerTask.cancel()

  def receive = idle

  def idle: Receive = {
    case WorkReady =>
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
      context.setReceiveTimeout(5.seconds)
      context.become(waitForWorkDoneAck(result))

    case JobExecutor.Failed(executionId, reason) =>
      reason match {
        case Right(throwable) =>
          log.error(throwable, "Execution {} has thrown an exception.", executionId)
        case _ =>
      }
      sendToMaster(WorkFailed(workerId, executionId, reason))
      context.setReceiveTimeout(5.seconds)
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

  def sendToMaster(msg: Any): Unit = {
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