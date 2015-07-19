package io.chronos.resolver

import java.net.{URL, URLClassLoader}

import io.chronos.id.JobModuleId
import io.chronos.{Job, JobClass}

import scala.util.Try

/**
 * Created by aalonsodominguez on 17/07/15.
 */
case class JobModulePackage(moduleId: JobModuleId, classpath: Seq[URL]) {

  private val classLoader = new URLClassLoader(classpath.toArray)

  def jobClass(className: String): Try[JobClass] = Try(classLoader.loadClass(className)).map { _.asInstanceOf[JobClass] }

  def newJob(className: String, params: Map[String, Any]): Try[Job] = jobClass(className).flatMap { jobClass =>
    Try(jobClass.newInstance()).map { job =>
      jobClass.getDeclaredFields.
        filter(field => params.contains(field.getName)).
        foreach { field =>
          val paramValue = params(field.getName)
          field.set(job, paramValue)
        }
      job
    }
  }

}
