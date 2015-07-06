package io.chronos.scheduler.worker

import java.util.UUID

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.contrib.pattern.ClusterClient.SendToAll
import io.chronos.scheduler.butler.Butler
import io.chronos.scheduler.id.{ExecutionId, JobId, WorkId}
import io.chronos.scheduler.protocol.WorkerProtocol

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

  val workerId = UUID.randomUUID().toString
  
  val registerTask = context.system.scheduler.schedule(
    0.seconds, registerInterval, clusterClient,
    SendToAll(Butler.Path, RegisterWorker(workerId))
  )

  val jobExecutor = context.watch(context.actorOf(jobExecutorProps, "exec"))
  
  private var currentWorkId: Option[WorkId] = None

  def workId: WorkId = currentWorkId match {
    case Some(id) => id
    case None     => throw new IllegalStateException("Not working")
  }

  def jobId: JobId = currentWorkId match {
    case Some((jobId, _)) => jobId
    case None             => throw new IllegalStateException("Not working")
  }

  def executionId: ExecutionId = currentWorkId match {
    case Some((_, executionId)) => executionId
    case None                   => throw new IllegalStateException("Not working")
  }

  override def postStop(): Unit = registerTask.cancel()

  def receive = idle

  def idle: Receive = {
    case WorkReady =>
      sendToMaster(RequestWork(workerId))

    case Work(workId) =>
      log.info("Received work for job {} on execution {}", jobId, executionId)
      currentWorkId = Some(workId)
      jobExecutor ! JobExecutor.ExecuteWork(workId)
      context.become(working)
  }

  def working: Receive = {
    case JobExecutor.CompletedWork(workId, result) =>
      log.info("Work is complete. Result {}.", result)
      sendToMaster(WorkDone(workerId, workId, result))
      context.setReceiveTimeout(5.seconds)
      context.become(waitForWorkDoneAck(result))

    case _: Work =>
      log.info("Yikes. Master told me to do work, while I'm working.")
  }

  def waitForWorkDoneAck(result: Any): Receive = {
    case WorkDoneAck(id) if id == workId =>
      sendToMaster(RequestWork(workerId))
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)

    case ReceiveTimeout =>
      log.info("No ack from master, retrying")
      sendToMaster(WorkDone(workerId, workId, result))
  }

  def sendToMaster(msg: Any): Unit = {
    clusterClient ! SendToAll(Butler.Path, msg)
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException => Stop
    case _: Exception =>
      currentWorkId.foreach {
        workId => sendToMaster(WorkFailed(workerId, workId))
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