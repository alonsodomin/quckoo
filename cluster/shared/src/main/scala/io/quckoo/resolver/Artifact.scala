/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
