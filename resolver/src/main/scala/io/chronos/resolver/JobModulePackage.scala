package io.chronos.resolver

import java.net.URL

import io.chronos.id.JobModuleId
import io.chronos.{Job, JobClass}
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.ClassRealm
import org.slf4s.Logging

import scala.util.Try

/**
 * Created by aalonsodominguez on 17/07/15.
 */
object JobModulePackage extends Logging {

  def apply(moduleId: JobModuleId, classpath: Seq[URL])(implicit classWorld: ClassWorld): JobModulePackage = {
    log.info(s"Module $moduleId classpath:")
    classpath.foreach { url =>
      log.info(url.toString)
    }
    val classRealmId = moduleId.toString
    val classRealm = Option(classWorld.getClassRealm(classRealmId)).
      getOrElse(classWorld.newRealm(classRealmId))
    classpath.foreach { classRealm.addURL }
    new JobModulePackage(moduleId, classpath, classRealm)
  }

}

class JobModulePackage private (val moduleId: JobModuleId, val classpath: Seq[URL], val classRealm: ClassRealm) {

  def jobClass(className: String): Try[JobClass] = Try(classRealm.loadClass(className)).map { _.asInstanceOf[JobClass] }

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
