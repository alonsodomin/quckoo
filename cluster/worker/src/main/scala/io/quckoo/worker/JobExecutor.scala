package io.quckoo.worker

import akka.actor.{Actor, ActorLogging, Props}
import io.quckoo.Task
import io.quckoo.fault.{ExceptionThrown, Fault}
import io.quckoo.resolver.{Artifact, Resolve}

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  final case class Execute(task: Task, artifact: Artifact)
  final case class Failed(error: Fault)
  final case class Completed(result: Any)

  def props: Props = Props(classOf[JobExecutor])
}

class JobExecutor extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case Execute(task, artifact) =>
      val result = for {
        job    <- artifact.newJob(task.jobClass, task.params)
        invoke <- Try(job.call())
      } yield invoke

      val response = result match {
        case Success(value) => Completed(value)
        case Failure(ex)    => Failed(ExceptionThrown(ex))
      }

      sender() ! response
  }

}

