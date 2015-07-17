package io.chronos.worker

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.id.ExecutionId
import io.chronos.{Job, JobClass, Work}
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(work: Work)
  case class Failed(executionId: ExecutionId, cause: Throwable)
  case class Completed(executionId: ExecutionId, result: Any)
  
  def props(classWorld: ClassWorld, moduleResolver: ModuleResolver): Props =
    Props(classOf[JobExecutor], classWorld, moduleResolver)
}

class JobExecutor(val classWorld: ClassWorld, val moduleResolver: ModuleResolver) extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case Execute(work) =>
      log.info(s"Resolving module ${work.moduleId}")
      val classRealm = resolveClassRealm(work.moduleId)
      val jobClass = classRealm.loadClass(work.jobClass).asInstanceOf[JobClass]

      log.info("Executing work. workId={}", work.executionId)
      val jobInstance = jobClass.newInstance()

      populateJobParams(jobClass, work.params, jobInstance)

      Try[Any](jobInstance.execute()) match {
        case Success(result) =>
          sender() ! Completed(work.executionId, result)
        case Failure(cause) =>
          sender() ! Failed(work.executionId, cause)
      }
  }

  private def resolveClassRealm(moduleId: JobModuleId): ClassRealm = {
    val realm = classWorld.newRealm(moduleId.toString)
    moduleResolver.resolveTransitive(moduleId).map(_.getArtifact).foreach { artifact =>
      realm.addURL(artifact.getFile.toURI.toURL)
    }
    realm
  }

  private def populateJobParams[T <: Job](jobClass: JobClass, params: Map[String, Any], jobInstance: T): Unit = {
    jobClass.getDeclaredFields.
      filter(field => params.contains(field.getName)).
      foreach { field =>
        val paramValue = params(field.getName)
        field.set(jobInstance, paramValue)
      }
  }

}
