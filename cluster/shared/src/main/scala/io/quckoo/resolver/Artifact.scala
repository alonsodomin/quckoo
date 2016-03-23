package io.quckoo.resolver

import java.net.URL
import java.util.concurrent.Callable

import io.quckoo.JobClass
import io.quckoo.id.ArtifactId
import org.slf4s.Logging

import scala.util.Try

/**
 * Created by aalonsodominguez on 17/07/15.
 */
object Artifact {

  def apply(moduleId: ArtifactId, classpath: Seq[URL]): Artifact = {
    new Artifact(moduleId, new ArtifactClassLoader(classpath.toArray))
  }

}

final class Artifact private[resolver] (val artifactId: ArtifactId, classLoader: ArtifactClassLoader) extends Logging {

  logCreation()

  private[resolver] def loadClass(className: String): Try[Class[_]] =
    Try(classLoader.loadClass(className))

  def classpath: Seq[URL] = classLoader.getURLs

  def jobClass(className: String): Try[JobClass] =
    loadClass(className) map { _.asInstanceOf[JobClass] }

  def newJob(className: String, params: Map[String, Any]): Try[Callable[_]] =
    jobClass(className).flatMap { jobClass =>
      Try(jobClass.newInstance()).map { job =>
        if (params.nonEmpty) {
          log.info("Injecting parameters into job instance.")
          jobClass.getDeclaredFields.
            filter(field => params.contains(field.getName)).
            foreach { field =>
              val paramValue = params(field.getName)
              field.set(job, paramValue)
            }
        }
        job
      }
    }

  override def equals(other: Any): Boolean = other match {
    case that: Artifact => artifactId == that.artifactId
    case _ => false
  }

  override def hashCode(): Int = artifactId.hashCode()

  private def logCreation(): Unit = {
    val classpathStr = classpath.mkString("::")
    log.debug(s"Job package created for artifact $artifactId and classpath: $classpathStr")
  }

}
