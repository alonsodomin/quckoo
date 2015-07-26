package io.chronos.resolver

import java.net.URL
import java.util.concurrent.Callable

import io.chronos.JobClass
import io.chronos.id.JobModuleId
import org.slf4s.Logging

import scala.util.Try

/**
 * Created by aalonsodominguez on 17/07/15.
 */
object JobModulePackage extends Logging {

  def apply(moduleId: JobModuleId, classpath: Seq[URL]): JobModulePackage = {
    val cp = classpath.mkString(":")
    log.info(s"Module $moduleId classpath: $cp")

    val classLoader = new JobModuleClassLoader(classpath.toArray, Thread.currentThread().getContextClassLoader)
    new JobModulePackage(moduleId, classLoader)
  }

}

class JobModulePackage private[resolver] (val moduleId: JobModuleId, classLoader: JobModuleClassLoader) extends Logging {

  private[resolver] def loadClass(className: String): Try[Class[_]] = Try(classLoader.loadClass(className))

  def classpath: Seq[URL] = classLoader.getURLs

  def jobClass(className: String): Try[JobClass] = loadClass(className) map { _.asInstanceOf[JobClass] }

  def newJob(className: String, params: Map[String, Any]): Try[Callable[_]] = jobClass(className).flatMap { jobClass =>
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

}
