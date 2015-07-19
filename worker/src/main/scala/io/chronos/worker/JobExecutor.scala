package io.chronos.worker

import akka.actor.{Actor, ActorLogging, Props}
import io.chronos.id.ExecutionId
import io.chronos.protocol.{ExecutionFailedCause, ResolutionFailed}
import io.chronos.resolver.JobModuleResolver
import io.chronos.{Job, JobClass, Work}
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm

import scala.util.{Failure, Success, Try}

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object JobExecutor {

  case class Execute(work: Work)

  case class Failed(executionId: ExecutionId, reason: ExecutionFailedCause)
  case class Completed(executionId: ExecutionId, result: Any)

  def props(classWorld: ClassWorld, moduleResolver: JobModuleResolver): Props =
    Props(classOf[JobExecutor], classWorld, moduleResolver)
}

class JobExecutor(val classWorld: ClassWorld, val moduleResolver: JobModuleResolver) extends Actor with ActorLogging {
  import JobExecutor._

  def receive = {
    case Execute(work) =>
      log.info(s"Resolving module ${work.moduleId}")

      findClassRealm(work) match {
        case Left(classRealm) =>
          val jobClass = classRealm.loadClass(work.jobClass).asInstanceOf[JobClass]

          log.info("Executing work. workId={}", work.executionId)
          Try(jobClass.newInstance()) map { jobInstance =>
            populateJobParams(jobClass, work.params, jobInstance)
            jobInstance
          } flatMap(job => Try(job.execute())) match {
            case Success(result) =>
              sender() ! Completed(work.executionId, result)
            case Failure(cause) =>
              sender() ! Failed(work.executionId, Right(cause))
          }

        case Right(resolutionFailed) =>
          sender() ! Failed(work.executionId, Left(resolutionFailed))
      }
  }

  private def findClassRealm(work: Work): Either[ClassRealm, ResolutionFailed] =
    Try(classWorld.getClassRealm(work.moduleId.toString)).toOption match {
      case Some(cr) => Left(cr)
      case None     =>
        moduleResolver.resolve(work.moduleId) match {
          case Left(jobPackage) =>
            val classRealm = classWorld.newRealm(work.moduleId.toString)
            jobPackage.classpath.foreach { classRealm.addURL }
            Left(classRealm)
          case Right(invalid) => _
    }
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
